package org.dcache.auth;


import java.io.Serializable;

public abstract class UserAuthBase implements Serializable {
    private static final long serialVersionUID = -7700110348980815506L;

    public transient long id;
    public String Username;
    public String DN;
    private FQAN fqan;
    public int priority;
    public int UID = -1;
    public int GID = -1;
    public String Home;
    public String Root;
    public String FsRoot;
    public boolean ReadOnly;

    public UserAuthBase(String user, String DN, String fqan, boolean readOnly,
                        int priority, int uid, int gid, String home,
                        String root, String fsroot) {
        Username = user;
        this.DN = DN;
        if(fqan != null) {
            this.fqan = new FQAN(fqan);
        } else if(user != null) {
            this.fqan = new FQAN(user);
        }
        ReadOnly = readOnly;
        this.priority = priority;
        UID = uid;
        GID = gid;
        Home = home;
        Root = root;
        FsRoot = fsroot;
    }

    public UserAuthBase(String user, boolean readOnly, int uid, int gid,
                        String home, String root, String fsroot) {
        this(user, null, null, readOnly, 0, uid, gid, home, root, fsroot);
    }

    /**
     * non-private default constructor to satisfy the JPA requirements
     */
    public UserAuthBase() {
    }

    public String readOnlyStr() {
        if(ReadOnly) {
            return "read-only";
        } else {
            return "read-write";
        }
    }

    abstract public boolean isAnonymous();
    abstract public boolean isWeak();

    public FQAN getFqan() {
        return fqan;
    }
}
