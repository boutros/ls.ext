package no.deichman.services.entity.patch;

import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Responsibility: TODO.
 */
public final class Patch {

    private String operation = null;
    private Statement statement = null;
    private String graph = null;

    public Patch(String operation, Statement statement, String graph){
        setOperation(operation);
        setStatement(statement);
        setGraph(graph);
    }

    public String getOperation() {
        return operation;
    }

    public Statement getStatement() {
        return statement;
    }

    public String getGraph() {
        return graph;
    }

    public void setOperation(String op) {
        operation = op;
    }

    public void setStatement(Statement st) {
        statement = st;
    }

    public void setGraph(String g) {
        graph = g;
    }

}