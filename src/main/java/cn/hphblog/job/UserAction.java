package cn.hphblog.job;

public class UserAction {
    private int Id;
    private String Name;
    private String Operation;
    private Long t;
    private int opt;

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getOperation() {
        return Operation;
    }

    public void setOperation(String operation) {
        Operation = operation;
    }

    public Long getT() {
        return t;
    }

    public void setT(Long t) {
        this.t = t;
    }

    public int getOpt() {
        return opt;
    }

    public void setOpt(int opt) {
        this.opt = opt;
    }

    public UserAction(int id, String name, String operation, Long t, int opt) {
        Id = id;
        Name = name;
        Operation = operation;
        this.t = t;
        this.opt = opt;
    }

    public UserAction() {
    }

    @Override
    public String toString() {
        return "UserAction{" +
                "Id=" + Id +
                ", Name='" + Name + '\'' +
                ", Operation='" + Operation + '\'' +
                ", t=" + t +
                ", opt=" + opt +
                '}';
    }
}
