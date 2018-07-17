package org.vadere.simulator.projects.migration;

public class MigrationResult {
    public int total;
    public int upToDate;
    public int legacy;
    public int nomigratable;

    public MigrationResult(){
    }

    public MigrationResult(int total){
        this.total = total;
    }

    boolean checkTotal(){
        return (upToDate + legacy + nomigratable) == total;
    }

    public MigrationResult add(MigrationResult other){
        this.total =+ other.total;
        this.upToDate =+ other.upToDate;
        this.legacy =+ other.legacy;
        this.nomigratable =+ other.nomigratable;
        return this;
    }
}
