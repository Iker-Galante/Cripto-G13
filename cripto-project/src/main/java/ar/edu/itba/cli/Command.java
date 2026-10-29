package ar.edu.itba.cli;

public enum Command {
    EMBED("-embed"),
    EXTRACT("-extract");

    private final String flag;

    Command(String flag) {
        this.flag = flag;
    }

    public String flag() {
        return flag;
    }
}
