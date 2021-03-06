/* MinerRunner.java
 *
 * Copyright (C) 2014 Florian Brosch
 *
 * Based on work from Andreas Mauczka
 *
 * This program is developed as part of the research project
 * "Lexical Repository Analyis" which is part of the PhD thesis
 * "Design and evaluation for identification, mapping and profiling
 * of medium sized software chunks" by Andreas Mauczka at
 * INSO - University of Technology Vienna. For questions in regard
 * to the research project contact andreas.mauczka(at)inso.tuwien.ac.at
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2.0
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * Author:
 *       Florian Brosch <flo.brosch@gmail.com>
 */

package at.ac.tuwien.inso.subcat.miner;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import at.ac.tuwien.inso.subcat.miner.MetaData.ParamType;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.ModelPool;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.utility.Reporter;


public class MinerRunner {
	private static LinkedList<MetaData> registeredMiner;

	private List<MinerListener> listeners = new LinkedList<MinerListener> ();
	private LinkedList<RunnableMiner> miners = new LinkedList<RunnableMiner> ();
	
	private ParameterException storedParameterException;
	private MinerException storedMinerException;

	private static void init () {
		if (registeredMiner == null) {
			registeredMiner = new LinkedList<MetaData> ();
			registeredMiner.add (new BugzillaMinerMetaData ());
			registeredMiner.add (new GitMinerMetaData ());
			registeredMiner.add (new SvnMinerMetaData ());
		}
	}
	
	private MinerRunner (ModelPool pool, Project project, Settings settings, Reporter reporter) throws MinerException {
		assert (settings != null);
		assert (project != null);
		assert (settings != null);
		assert (reporter != null);

		init ();

		Model model = null;
		try {
			model = pool.getModel ();
		} catch (SQLException e) {
			throw new MinerException ("SQL-Error: " + e.getMessage (), e);
		} finally {
			if (model != null) {
				model.close ();
				model = null;
			}
		}


		LinkedList<Miner.MinerType> foundMinerTypes = new LinkedList<Miner.MinerType> ();
		
		for (MetaData meta : registeredMiner) {
			if (foundMinerTypes.contains (meta.getType ())) {
				continue ;
			}

			if (meta.is (project, settings)) {
				Miner miner = meta.create (settings, project, pool, reporter);
				miner.addListener (listeners);
				this.miners.add (new RunnableMiner (this, miner));
				foundMinerTypes.add (meta.getType ());
			}
		}

		if (settings.bugRepository != null || settings.bugProductName != null) {
			if (!foundMinerTypes.contains (Miner.MinerType.BUG)) {
				throw new MinerException ("No fitting bug repository miner found.");
			}
		}
		if (settings.srcLocalPath != null) {
			if (!foundMinerTypes.contains (Miner.MinerType.SOURCE)) {
				throw new MinerException ("No fitting source repository miner found.");
			}
		}
	}

	public void run () throws MinerException, ParameterException {
		for (RunnableMiner thread : miners) {
			thread.start ();
		}

		for (RunnableMiner thread : miners) {
			try {
				thread.join ();
			} catch (InterruptedException e) {
			}
		}

		if (storedMinerException != null) {
			throw storedMinerException;
		}		

		if (storedParameterException != null) {
			throw storedParameterException;
		}		
	}

	synchronized void stop (MinerException e) {
		if (storedMinerException == null) {
			storedMinerException = e;
		}

		for (RunnableMiner miner : this.miners) {
			miner.interrupt ();
		}
	}


	synchronized void stop (ParameterException e) {
		if (storedParameterException == null) {
			storedParameterException = e;
		}

		for (RunnableMiner miner : this.miners) {
			miner.interrupt ();
		}
	}

	public synchronized void addListener (MinerListener listener) {
		assert (listener != null);

		listeners.add (listener);

		for (RunnableMiner miner : miners) {
			miner.getMiner ().addListener (listener);
		}
	}

	public synchronized void removeListener (MinerListener listener) {
		assert (listener != null);

		listeners.remove (listener);
		
		for (RunnableMiner miner : miners) {
			miner.getMiner ().removeListener (listener);
		}
	}

	public static void main (String[] args) {
		Options options = new Options ();
		options.addOption ("h", "help", false, "Show this options");
		options.addOption ("m", "miner-help", false, "Show miner specific options");
		options.addOption ("d", "db", true, "The database to process (required)");
		options.addOption ("p", "project", true, "The project ID to process");
		options.addOption ("P", "list-projects", false, "List all registered projects");
		options.addOption ("v", "verbose", false, "Show details");

		options.addOption (null, "bug-repo", true, "Bug Repository URL");
		options.addOption (null, "bug-product", true, "Bug Product Name");
		options.addOption (null, "bug-tracker", true, "Bug Tracker name (e.g. bugzilla)");
		options.addOption (null, "bug-account", true, "Bug account name");
		options.addOption (null, "bug-passwd", true, "Bug account password");
		options.addOption (null, "bug-enable-untrusted", false, "Accept untrusted certificates");
		options.addOption (null, "bug-threads", true, "Thread count used in bug miners");
		options.addOption (null, "bug-cooldown-time", true, "Bug cooldown time");
		options.addOption (null, "bug-miner-option", true, "Bug miner specific option. Format: <option-name>:value");
		options.addOption (null, "bug-update", false, "Mine all changes since the last run");
		options.getOption ("bug-miner-option").setArgs(Option.UNLIMITED_VALUES);

		options.addOption (null, "src-path", true, "Local source repository path");
		options.addOption (null, "src-remote", true, "Remote address");
		options.addOption (null, "src-passwd", true, "Source repository account password");
		options.addOption (null, "src-account", true, "Source repository account name");
		options.addOption (null, "src-miner-option", true, "Source miner specific option. Format: <option-name>:value");
		options.getOption ("src-miner-option").setArgs(Option.UNLIMITED_VALUES);

		final Reporter reporter = new Reporter (true);
		reporter.startTimer ();

		boolean printTraces = false;
		Settings settings = new Settings ();
		ModelPool pool = null;

		CommandLineParser parser = new PosixParser ();
		
		try {
			CommandLine cmd = parser.parse (options, args);

			if (cmd.hasOption ("help")) {
				HelpFormatter formatter = new HelpFormatter ();
				formatter.printHelp ("postprocessor", options);
				return ;
			}
			

			if (cmd.hasOption ("miner-help")) {
				init ();
				for (MetaData meta : registeredMiner) {
					System.out.println (meta.name ());
					for (Entry<String, ParamType> opt : meta.getSpecificParams ().entrySet()) {
						System.out.println ("  - " + opt.getKey() + " (" + opt.getValue() + ")");
					}
				}
				return ;
			}


			if (cmd.hasOption ("db") == false) {
				reporter.error ("miner", "Option --db is required");
				reporter.printSummary ();
				return ;
			}			

			printTraces = cmd.hasOption ("verbose");
			pool = new ModelPool (cmd.getOptionValue ("db"), 2);

			if (cmd.hasOption ("list-projects")) {
				Model model = pool.getModel ();

				for (Project proj : model.getProjects ()) {
					System.out.println ("  " + proj.getId () + ": " + proj.getDate ());
				}

				model.close ();
				return ;
			}



			Project project;
			Model model = pool.getModel ();
			boolean hasSrcInfos = false;
			boolean hasBugInfos = false;
			if (cmd.hasOption ("project")) {
				try {
					int projId = Integer.parseInt(cmd.getOptionValue ("project"));
					project = model.getProject (projId);
				} catch (NumberFormatException e) {
					reporter.error ("post-processor", "Invalid project ID");
					reporter.printSummary ();
					return ;
				}

				if (project == null) {
					reporter.error ("post-processor", "Invalid project ID");
					reporter.printSummary ();
					return ;
				}

				List<String> flags = model.getFlags (project);
				hasBugInfos = flags.contains (Model.FLAG_BUG_INFO);
				hasSrcInfos = flags.contains (Model.FLAG_SRC_INFO);
			} else {
				project = model.addProject (new Date (), null, settings.bugTrackerName, settings.bugRepository, settings.bugProductName, null);
			}
			model.close ();


			//
			// Source Repository Mining:
			//

			settings.srcLocalPath = cmd.getOptionValue ("src-path");
			settings.srcRemote = cmd.getOptionValue ("src-remote");
			settings.srcRemotePw = cmd.getOptionValue ("src-passwd");
			settings.srcRemoteUser = cmd.getOptionValue ("src-account");


			if (settings.srcRemotePw == null || settings.srcRemoteUser == null) {
				if (settings.srcRemotePw != null) {
					reporter.error ("miner", "--src-passwd should only be used in combination with --src-account");
					reporter.printSummary ();
					return ;
				} else if (settings.srcRemoteUser != null) {
					reporter.error ("miner", "--src-account should only be used in combination with --src-passwd");
					reporter.printSummary ();
					return ;
				}
			}

			if (settings.srcRemoteUser != null && settings.srcRemote == null) {
				reporter.error ("miner", "--src-account should only be used in combination with --src-remote");
				reporter.printSummary ();
				return ;
			}


			if (settings.srcLocalPath != null && hasSrcInfos) {
				reporter.error ("miner", "Source repository updates are not supported yet.");
				reporter.printSummary (true);
				return ;
			}
			
			
			//
			// Bug Repository Mining:
			//

			settings.bugRepository = cmd.getOptionValue ("bug-repo");
			settings.bugProductName = cmd.getOptionValue ("bug-product");
			settings.bugTrackerName = cmd.getOptionValue ("bug-tracker");
			settings.bugEnableUntrustedCertificates = cmd.hasOption ("bug-enable-untrusted");
			settings.bugLoginUser = cmd.getOptionValue ("bug-account");
			settings.bugLoginPw = cmd.getOptionValue ("bug-passwd");


			settings.bugUpdate = cmd.hasOption ("bug-update");
			boolean includeBugs = false;
			if (settings.bugUpdate) {
				if (project.getBugTracker () == null || project.getDomain () == null) {
					reporter.error ("miner", "flag --bug-update is requires previously mined bugs. Use --bug-repository, --bug-product and --bug-tracker.");
					reporter.printSummary (true);
					return ;
				}
				if (settings.bugTrackerName != null) {
					reporter.warning ("miner", "flag --bug-tracker without effect");
				}
				if (settings.bugProductName != null) {
					reporter.warning ("miner", "flag --bug-product without effect");
				}
				if (settings.bugRepository != null) {
					reporter.warning ("miner", "flag --bug-repository without effect");
				}

				settings.bugTrackerName = project.getBugTracker ();
				settings.bugProductName = project.getProduct ();
				settings.bugRepository = project.getDomain ();
				includeBugs = true;
			} else if (settings.bugRepository != null && settings.bugProductName != null && settings.bugTrackerName != null) {
				if (hasBugInfos == false) {
					// The user is trying to append bug tracker information to a existing project.
				} else if (!settings.bugTrackerName.equalsIgnoreCase (project.getBugTracker ())
					|| !settings.bugProductName.equalsIgnoreCase (project.getProduct ())
					|| !settings.bugRepository.equalsIgnoreCase (project.getDomain ()))
				{
					reporter.error ("miner", "There are already previously mined bugs for this project. Use --bug-update to update the database.");
					reporter.printSummary (true);
					return ;
				}
				if (settings.bugRepository == null) {
					reporter.error ("miner", "flag --bug-repository is required");
					reporter.printSummary (true);
					return ;
				}
				if (settings.bugProductName == null) {
					reporter.error ("miner", "flag --bug-product is required");
					reporter.printSummary (true);
					return ;
				}
				includeBugs = true;
			}

			if (includeBugs) {
				if (settings.bugLoginPw == null || settings.bugLoginUser == null) {
					if (settings.bugLoginPw != null) {
						reporter.error ("miner", "--bug-passwd should only be used in combination with --bug-account");
						reporter.printSummary ();
						return ;
					} else if (settings.bugLoginUser != null) {
						reporter.error ("miner", "--bug-account should only be used in combination with --bug-passwd");
						reporter.printSummary ();
						return ;
					}
				}

				if (cmd.hasOption ("bug-threads")) {
					try {
						settings.bugThreads = Integer.parseInt (cmd.getOptionValue ("bug-threads"));
					} catch (Exception e) {
						reporter.error ("miner", "--bug-threads: Invalid parameter type");
						reporter.printSummary ();
						return ;
					}
				}

				if (cmd.hasOption ("bug-cooldown-time")) {
					try {
						settings.bugCooldownTime = Integer.parseInt (cmd.getOptionValue ("bug-cooldown-time"));
					} catch (Exception e) {
						reporter.error ("miner", "--bug-cooldown-time: Invalid parameter type");
						reporter.printSummary ();
						return ;
					}
				}

				if (cmd.hasOption ("bug-miner-option")) {
					for (String str : cmd.getOptionValues ("bug-miner-option")) {
						addSpecificParameter (settings.bugSpecificParams, str);
					}
				}

				if (cmd.hasOption ("src-miner-option")) {
					for (String str : cmd.getOptionValues ("src-miner-option")) {
						addSpecificParameter (settings.srcSpecificParams, str);
					}				
				}
			} else {
				if (settings.bugLoginPw != null) {
					reporter.error ("miner", "--bug-passwd should only be used in combination with --bug-account");					
					reporter.printSummary ();
					return ;
				}
				if (settings.bugLoginUser != null) {
					reporter.error ("miner", "--bug-account should only be used in combination with --bug-account");
					reporter.printSummary ();
					return ;
				}
				if (settings.bugRepository != null) {
					reporter.error ("miner", "--bug-repo should only be used in combination with --bug-product and --bug-tracker");
					reporter.printSummary ();
					return ;
				}
				if (settings.bugProductName != null) {
					reporter.error ("miner", "--bug-product should only be used in combination with --bug-repo and --bug-tracker");
					reporter.printSummary ();
					return ;
				}
				if (settings.bugTrackerName != null) {
					reporter.error ("miner", "--bug-tracker should only be used in combination with --bug-repo and --bug-product");
					reporter.printSummary ();
					return ;
				}
				if (settings.bugEnableUntrustedCertificates) {
					reporter.error ("miner", "--bug-enable-untrusted should only be used in combination with --bug-repo, --bug-tracker and --bug-product");
					reporter.printSummary ();
					return ;
				}
				if (settings.bugUpdate) {
					reporter.error ("miner", "--bug-update should only be used in combination with --bug-repo, --bug-tracker and --bug-product");
					reporter.printSummary ();
					return ;
				}
				if (cmd.hasOption ("bug-threads")) {
					reporter.error ("miner", "--bug-threads should only be used in combination with --bug-repo, --bug-tracker and --bug-product");
					reporter.printSummary ();
					return ;
				}
				if (cmd.hasOption ("bug-cooldown-time")) {
					reporter.error ("miner", "--bug-cooldown-time should only be used in combination with --bug-repo, --bug-tracker and --bug-product");
					reporter.printSummary ();
					return ;
				}
				if (cmd.hasOption ("bug-miner-option")) {
					reporter.error ("miner", "--bug-miner-option should only be used in combination with --bug-repo, --bug-tracker and --bug-product");
					reporter.printSummary ();
					return ;
				}
			}

			
			
			//
			// Run:
			//
			
			MinerRunner runner = new MinerRunner (pool, project, settings, reporter);
			if (cmd.hasOption ("verbose")) {
				runner.addListener (new MinerListener () {
					private Map<Miner, Integer> totals = new HashMap<Miner, Integer> ();
					
					@Override
					public void start (Miner miner) {
					}
		
					@Override
					public void end (Miner miner) {
					}
		
					@Override
					public void stop (Miner miner) {
					}

					@Override
					public void tasksTotal (Miner miner, int count) {
						totals.put (miner, count);
					}

					@Override
					public void tasksProcessed (Miner miner, int processed) {
						Integer total = totals.get (miner);
						reporter.note (miner.getName (), "status: " + processed + "/" + ((total == null)? "0" : total));
					}
				});
			}

			runner.run ();
		} catch (ParameterException e) {
			reporter.error (e.getMiner ().getName (), e.getMessage ());
		} catch (ParseException e) {
			reporter.error ("miner", "Parsing failed: " + e.getMessage ());
			if (printTraces == true) {
				e.printStackTrace ();
			}
		} catch (ClassNotFoundException e) {
			reporter.error ("miner", "Failed to create a database connection: " + e.getMessage ());
			if (printTraces == true) {
				e.printStackTrace ();
			}
		} catch (SQLException e) {
			reporter.error ("miner", "Failed to create a database connection: " + e.getMessage ());
			if (printTraces == true) {
				e.printStackTrace ();
			}
		} catch (MinerException e) {
			reporter.error ("miner", "Mining Error: " + e.getMessage ());
			if (printTraces == true) {
				e.printStackTrace ();
			}
		} finally {
			if (pool != null) {
				pool.close ();
			}
		}

		reporter.printSummary (true);
	}

	private static void addSpecificParameter (Map<String, String> map, String str) throws MinerException {
		String[] args = str.split("=", 2);
		if (args.length != 2) {
			throw new MinerException ("Invalid specific parameter format. Got: '" + str + "', expected '<name>=<value>'");
		}

		map.put (args[0], args[1]);
	}
}
