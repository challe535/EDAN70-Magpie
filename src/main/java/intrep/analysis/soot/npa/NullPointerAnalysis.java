package intrep.analysis.soot.npa;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.Local;
import soot.Unit;
import soot.jimple.*;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class NullPointerAnalysis extends ForwardFlowAnalysis<Unit, HashSet<Local>> {
    private HashSet<Local> params;

    public NullPointerAnalysis(DirectedGraph<Unit> graph, List<Local> paramLocals) {
        super(graph);
        params = new HashSet<>();

        for(Local l : paramLocals)
            params.add(l);

        doAnalysis();
    }

    @Override
    protected void flowThrough(HashSet<Local> inSet, Unit unit, HashSet<Local> outSet) {
        outSet.addAll(inSet);
        kill(outSet, unit);
        generate(inSet, unit, outSet);
    }

    @Override
    protected HashSet<Local> newInitialFlow() {
        return new HashSet<Local>();
    }

    @Override
    protected void merge(HashSet<Local> inSet1, HashSet<Local> inSet2, HashSet<Local> outSet) {
        inSet1.addAll(inSet2);
        outSet.addAll(inSet1);
    }

    @Override
    protected void copy(HashSet<Local> source, HashSet<Local> dest) {
        dest = new HashSet<>(source);
    }

    protected void kill(HashSet<Local> outSet, Unit unit) {
        unit.apply(new AbstractStmtSwitch() {
            @Override
            public void caseAssignStmt(AssignStmt stmt) {
                Local leftOp = (Local) stmt.getLeftOp();
                outSet.remove(leftOp);
            }
        });
    }

    protected void generate(Set<Local> inSet, Unit unit, Set<Local> outSet) {
        unit.apply(new AbstractStmtSwitch() {
            @Override
            public void caseAssignStmt(AssignStmt stmt) {
                Local leftOp = (Local) stmt.getLeftOp();
                stmt.getRightOp().apply(new AbstractJimpleValueSwitch() {
                    @Override
                    public void caseLocal(Local v) {
                        if (inSet.contains(v))
                            outSet.add(leftOp);
                    }

                    @Override
                    public void caseNullConstant(NullConstant v) {
                        outSet.add(leftOp);
                    }
                });
            }

            @Override
            public void caseIdentityStmt(IdentityStmt stmt) {
                Local leftOp = (Local) stmt.getLeftOp();
                if(!params.contains(leftOp))
                    outSet.add(leftOp);
            }
        });
    }
}
