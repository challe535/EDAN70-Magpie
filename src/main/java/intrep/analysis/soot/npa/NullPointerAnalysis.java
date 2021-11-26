package intrep.analysis.soot.npa;

import java.util.HashSet;
import java.util.Set;

import soot.Local;
import soot.Unit;
import soot.jimple.*;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

public class NullPointerAnalysis extends ForwardFlowAnalysis<Unit, Set<Local>> {
    enum AnalysisMode {
        MUST,
        MAY_P,
        MAY_O
    }
    AnalysisMode analysisMode;
    
    public NullPointerAnalysis(DirectedGraph graph, AnalysisMode analysisMode) {
        super(graph);
        this.analysisMode = analysisMode;
        doAnalysis();
    }

    @Override
    protected void flowThrough(Set<Local> inSet, Unit unit, Set<Local> outSet) {
        copy(inSet, outSet);
        kill(inSet, unit, outSet);
        generate(inSet, unit, outSet);
    }

    @Override
    protected Set<Local> newInitialFlow() {
        return new HashSet<Local>();
    }


    @Override
    protected void merge(Set<Local> inSet1, Set<Local> inSet2, Set<Local> outSet) {
        if(analysisMode != AnalysisMode.MUST) {
            outSet.addAll(inSet1);
            outSet.addAll(inSet2);
        } else {
            for (Local local : inSet1) {
                if(inSet2.contains(local))
                    outSet.add((Local)local.clone());
            }
        }
    }

    @Override
    protected void copy(Set<Local> source, Set<Local> dest) {
        for (Local local : source) {
            dest.add((Local)local.clone());
        }
    }

    protected void kill(Set<Local> inSet, Unit unit, Set<Local> outSet){
        unit.apply(new AbstractStmtSwitch() {
            @Override
            public void caseAssignStmt(AssignStmt stmt) {
                Local leftOp = (Local) stmt.getLeftOp();
                outSet.remove(leftOp);
            }
        });
    }

    protected void generate(Set<Local> inSet, Unit unit, Set<Local> outSet){
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

                    @Override
                    public void caseInterfaceInvokeExpr(InterfaceInvokeExpr v) {
                        if(analysisMode == AnalysisMode.MAY_P)
                            outSet.add(leftOp);
                    }

                    @Override
                    public void caseStaticInvokeExpr(StaticInvokeExpr v) {
                        if(analysisMode == AnalysisMode.MAY_P)
                            outSet.add(leftOp);
                    }

                    @Override
                    public void caseVirtualInvokeExpr(VirtualInvokeExpr v) {
                        if(analysisMode == AnalysisMode.MAY_P)
                            outSet.add(leftOp);
                    }
                });
            }

            @Override
            public void caseIdentityStmt(IdentityStmt stmt) {

                Local leftOp = (Local) stmt.getLeftOp();
                if(analysisMode == AnalysisMode.MAY_P)
                    if(!(stmt.getRightOp() instanceof ThisRef))
                        outSet.add(leftOp);
            }
        });
    }
}

