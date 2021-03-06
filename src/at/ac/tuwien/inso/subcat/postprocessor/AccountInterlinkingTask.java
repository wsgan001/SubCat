package at.ac.tuwien.inso.subcat.postprocessor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import at.ac.tuwien.inso.subcat.model.Identity;
import at.ac.tuwien.inso.subcat.model.Model;
import at.ac.tuwien.inso.subcat.model.Project;
import at.ac.tuwien.inso.subcat.model.User;
import at.ac.tuwien.inso.subcat.utility.BkTree;
import at.ac.tuwien.inso.subcat.utility.BkTree.BkNode;
import at.ac.tuwien.inso.subcat.utility.BkTree.Foreach;
import at.ac.tuwien.inso.subcat.utility.BkTree.LevensteinFunc;
import at.ac.tuwien.inso.subcat.utility.phonetic.HashFunc;


public class AccountInterlinkingTask extends PostProcessorTask {

	private static class UserContainer {
		public LinkedList<IdentityContainer> identities;
		
		public UserContainer () {
			identities = new LinkedList<IdentityContainer> ();
		}

		public String getName () {
			if (identities.size () == 0) {
				return null;
			}

			Identity tmp = identities.get (0).identity;
			return (tmp.getName () != null)? tmp.getName () : tmp.getMail ();
		}
	}
	
	private static class IdentityContainer {
		public UserContainer container;
		public Identity identity;

		public IdentityContainer (Identity identity) {
			this.identity = identity;
			this.container = null;
		}

	    @Override
	    public boolean equals (Object obj) {
	        return obj instanceof IdentityContainer && identity.equals(((IdentityContainer) obj).identity);
	    }

	    @Override
	    public int hashCode() {
	        return identity.hashCode ();
	    }
	}


	private HashFunc hashFunc = null;
	private int distance = 1;


	public AccountInterlinkingTask () {
		super (PostProcessorTask.BEGIN);
	}


	public void setDistance (int dist) {
		this.distance = dist;
	}

	public void setHashFunc (HashFunc func) {
		this.hashFunc = func;
	}
	
	
	@Override
	public void begin (PostProcessor processor) throws PostProcessorException {
		Model model = null;
		try {
			Project project = processor.getProject ();
			model = processor.getModelPool ().getModel ();


			Collection<UserContainer> users;
			if (hashFunc != null) {
				users = smartMatching (model, project);
			} else {
				users = new LinkedList<UserContainer> ();
				for (Identity identity : model.getIdentities (project)) {
					UserContainer cnt = new UserContainer ();
					cnt.identities.add (new IdentityContainer (identity));
					users.add (cnt);
				}
			}


			// Mails:
			users = mailMatching (users);

	
			// Update:
			model.begin ();
			model.removeUsers (project);

			for (UserContainer uc : users) {
				User user = model.addUser (project, uc.getName ());
				for (IdentityContainer idc : uc.identities) {
					model.setIdentityUser (idc.identity, user);
				}
			}
			model.commit ();
		} catch (SQLException e) {
			if (model != null) {
				try {
					model.rollback ();
				} catch (SQLException e1) {
				}
			}

			throw new PostProcessorException (e);
		} finally {
			if (model != null) {
				model.close ();
			}
		}
	}

	private Collection<UserContainer> mailMatching (Collection<UserContainer> users) {
		HashMap<String, UserContainer> index = new HashMap<String, UserContainer> ();
		LinkedList<UserContainer> etc = new LinkedList<UserContainer> ();

		for (UserContainer usr : users) {
			boolean emailFound = false;

			LinkedList<IdentityContainer> lst = usr.identities;
			for (IdentityContainer id : lst) {
				String mail = id.identity.getMail ();
				if (mail == null) {
					continue;
				}
				
				mail = mail.toLowerCase ();
				emailFound = true;

				UserContainer old = index.put (mail, usr);
				if (old != null && old != usr) {
					old.identities.addAll (usr.identities);
					usr.identities = old.identities;
				}
			}

			if (emailFound == false) {
				etc.add (usr);
			}
		}

		HashSet<UserContainer> res = new HashSet<UserContainer> (index.values ());
		etc.addAll (res);
		return etc;
	}
	
	private Collection<UserContainer> smartMatching (Model model, Project project) throws SQLException {
		final Collection<UserContainer> users = new LinkedList<UserContainer> ();

		// Index:
		final BkTree<IdentityContainer> tree = new BkTree<IdentityContainer> (new LevensteinFunc ());
		for (Identity identity : model.getIdentities (project, Model.CONTEXT_SRC)) {
			Set<String> fragments = identity.getNameFragments ();

			if (fragments.size () > 1) {
				IdentityContainer identityContainer = new IdentityContainer (identity);
				for (String fragment : fragments) {
					tree.add (this.hashFunc.hash (fragment), identityContainer);
				}
			}
		}


		// Bug:
		for (Identity identity : model.getIdentities (project, Model.CONTEXT_BUG)) {
			Set<String> fragments = identity.getNameFragments ();

			Collection<IdentityContainer> links = findIdentities (tree, fragments.toArray (new String[fragments.size ()]));
			groupIdentities (users, links, identity);
		}


		// Source:
		tree.foreach (new Foreach<IdentityContainer> () {

			private boolean cmp (Set<String> frags1, Set<String> frags2) {
				for (String frag1 : frags1) {
					for (String frag2 : frags2) {
						String[] dist1 = hashFunc.hash (frag1);
						String[] dist2 = hashFunc.hash (frag2);
						if (tree.distFunc.distance (dist1, dist2) <= distance) {
							return true;
						}
					}
				}
				
				return false;
			}
			
			@Override
			public void callback (BkNode<IdentityContainer> node) {
				if (node.values.size () == 0) {
					return ;
				}

				if (node.values.size () == 1) {
					IdentityContainer tmp = node.values.get (0);
					if (tmp.container == null) {
						tmp.container = new UserContainer ();
						tmp.container.identities.add (tmp);
						users.add (tmp.container);
					}
					return ;
				}

				for (int i = 0; i < node.values.size (); i++) {
					IdentityContainer id1 = node.values.get (i);
					Set<String> fragments1 = id1.identity.getNameFragments ();
					fragments1.remove (node.key);

					for (int y = i + 1; y < node.values.size (); y++) {
						IdentityContainer id2 = node.values.get (y);
						Set<String> fragments2 = id2.identity.getNameFragments ();
						fragments2.remove (node.key);

						if (cmp (fragments1, fragments2)) {
							if (id1.container == null && id2.container == null) {
								id1.container = new UserContainer ();
								id2.container = id1.container;
								users.add (id1.container);
								
								id1.container.identities.add (id1);
								id1.container.identities.add (id2);
							} else if (id1.container == null) {
								id1.container = id2.container;
								id1.container.identities.add (id1);
							} else if (id2.container == null) {
								id2.container = id1.container;
								id2.container.identities.add (id2);
							}
						}
					}
				}
			}
		});

		return users;
	}
	
	private void groupIdentities (Collection<UserContainer> users, Collection<IdentityContainer> links, Identity identity) {
		UserContainer user = new UserContainer ();
		IdentityContainer container = new IdentityContainer (identity);
		user.identities.add (container);
		container.container = user;
		users.add (user);

		for (IdentityContainer link : links) {
			if (link.container == null) {
				user.identities.add (link);
				link.container = user;
			}
		}
	}

    private Collection<IdentityContainer> findIdentities (BkTree<IdentityContainer> tree, String[] names) {
    	ArrayList<IdentityContainer> result = new ArrayList<IdentityContainer> ();
    	if (names.length < 2) {
    		// Not enough evidence
    		return result;
    	}

    	ArrayList<Set<IdentityContainer>> multiset = new ArrayList<Set<IdentityContainer>> (names.length);
    	for (int i = 0; i < names.length ; i++) {
    		multiset.add (tree.getSet (this.hashFunc.hash (names[i]), this.distance));
    	}
    	
    	for (int i = 0; i < multiset.size () - 1; i++) {
    		for (int j = i+1; j < multiset.size (); j++) {
    			HashSet<IdentityContainer> set = new HashSet<IdentityContainer> ();
    			set.addAll (multiset.get (i));
    			set.retainAll (multiset.get (j));
    			result.addAll (set);
    		}
    	}
    	
    	return result;
    }

	@Override
	public String getName () {
		return "account-interlinking";
	}
}
