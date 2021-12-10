package intrep.eval;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.ibm.wala.classLoader.Module;
import intrep.core.AnalysisFramework;
import intrep.core.CodeAnalysis;
import intrep.core.IntraJFramework;
import intrep.core.SootFramework;
import intrep.core.magpiebridge.StaticServerAnalysis;
import magpiebridge.core.AnalysisConsumer;
import magpiebridge.core.MagpieServer;

public class Evaluator {
    private static final Logger LOG = Logger.getLogger("main");

    private int iterations = 1;
    private final int extraIters = 100; // Used to discard first few measurements

    private double[] timings;

    private enum EvalName {
        Base,
        Compile,
        Analyze
    }

    private StaticServerAnalysis analyzer;

    public Evaluator(StaticServerAnalysis analyzer, int iterations) {
        this.analyzer = analyzer;
        this.iterations = iterations + extraIters;
    }

    public void eval(Collection<? extends Module> files, AnalysisConsumer consumer) {
        EvalName[] evalIterationNames = { EvalName.Base, EvalName.Compile, EvalName.Analyze };
        HashMap<AnalysisFramework, String[]> frameworksToEvaluate = new HashMap<>();

        String[] intrajAnalyses = { "NPA" };
        frameworksToEvaluate.put(new IntraJFramework(), intrajAnalyses);

        String[] sootAnalyses = { "SootNPA" };
        frameworksToEvaluate.put(new SootFramework(), sootAnalyses);

        // Clear enabled analyses
        clearActiveAnalyses();

        // Do this once before everything else because it takes a while first time.
        // This ensures a more fair comparison.
        analyzer.setClassPath((MagpieServer) consumer, files);

        // Start evaluating
        int counter = 0;
        int totalIterations = iterations * evalIterationNames.length * frameworksToEvaluate.size();
        double totalEvalTime = 0;
        Locale locale = new Locale("en");
        LOG.info("\n==================\nSTARTING ANALYSIS\n==================\n");
        for (AnalysisFramework fw : frameworksToEvaluate.keySet()) {
            analyzer.framework = fw;

            // Enable relevant analyses
            for (String an : frameworksToEvaluate.get(fw))
                StaticServerAnalysis.activeAnalyses.put(an, true);

            for (EvalName iterName : evalIterationNames) {
                timings = new double[iterations];
                for (int i = 0; i < iterations; i++) {
                    counter++;
                    double progress = 100.0 * (double) counter / (double) totalIterations;
                    LOG.info(String.format(locale, "Eval progress: %1.2f%%", progress));

                    EvalMethod em = null;

                    switch (iterName) {
                        case Base:
                            em = this::evalBase;
                            break;
                        case Compile:
                            em = this::evalCompile;
                            break;
                        case Analyze:
                            em = this::evalAnalyze;
                            break;
                        default:
                            LOG.severe("No eval stage found called " + iterName.toString());
                    }

                    double time = em.eval(files, consumer);
                    // if analysis iteration fails - redo iteration
                    if (time < 0.0) {
                        i--;
                        continue;
                    }

                    // record time
                    timings[i] = time;
                }

                // Write recorded data to file
                SimpleFileWriter writer = new SimpleFileWriter(fw.frameworkName() + "-" + iterName.toString() + ".txt");
                for (int i = extraIters; i < iterations; i++) {
                    writer.appendLn(String.format(locale, "%1.5f", timings[i] / 1000000.0));
                }
                writer.close();

                for (int i = 0; i < iterations; i++)
                    totalEvalTime += timings[i];
            }
        }
        LOG.info("\n==================\nFINISHED ANALYSIS in " + (totalEvalTime / 1000.0)
                + "seconds\n==================\n");
    }

    private double evalBase(Collection<? extends Module> files, AnalysisConsumer consumer) {
        double time = System.nanoTime();

        MagpieServer server = (MagpieServer) consumer;
        analyzer.setClassPath(server, files);

        // ANALYZE
        // Clean up previous analysis results and ongoing analyses
        server.cleanUp();
        for (Future<?> f : analyzer.last) {
            if (f != null && !f.isDone()) {
                f.cancel(false);
            }
        }
        analyzer.last.clear();

        // Initiate analyses on seperate threads and set them running
        for (CodeAnalysis analysis : StaticServerAnalysis.analysisList) {
            if (!StaticServerAnalysis.activeAnalyses.get(analysis.getName()))
                continue;

            analyzer.last.add(analyzer.exeService.submit(new Runnable() {
                @Override
                public void run() {
                    // Do nothing
                }
            }));
        }

        return System.nanoTime() - time;
    }

    private double evalCompile(Collection<? extends Module> files, AnalysisConsumer consumer) {
        MagpieServer server = (MagpieServer) consumer;
        analyzer.setClassPath(server, files);

        // Setup analysis framework and run
        double time = System.nanoTime();
        analyzer.framework.setup(files, analyzer.totalClassPath, analyzer.srcPath, analyzer.libPath,
                analyzer.progFilesAbsPaths);

        int exitCode = analyzer.framework.run();

        if (exitCode == 4) {
            return -1.0;
        }

        return System.nanoTime() - time;
    }

    private double evalAnalyze(Collection<? extends Module> files, AnalysisConsumer consumer) {
        MagpieServer server = (MagpieServer) consumer;

        // ANALYZE
        // Clean up previous analysis results and ongoing analyses
        server.cleanUp();
        for (Future<?> f : analyzer.last) {
            if (f != null && !f.isDone()) {
                f.cancel(false);
            }
        }
        analyzer.last.clear();

        double time = System.nanoTime();

        // Initiate analyses on seperate threads and set them running
        for (CodeAnalysis analysis : StaticServerAnalysis.analysisList) {
            if (!StaticServerAnalysis.activeAnalyses.get(analysis.getName()))
                continue;

            analyzer.last.add(analyzer.exeService.submit(new Runnable() {
                @Override
                public void run() {
                    analyzer.doAnalysisThread(files, server, analysis);
                }
            }));
        }

        // Wait for all analysis threads
        boolean done = false;
        while (!done) {
            boolean temp = true;
            for (Future<?> f : analyzer.last) {
                temp &= f.isDone();
            }
            done = temp;
        }

        return System.nanoTime() - time;
    }

    private void clearActiveAnalyses() {
        for (String a : StaticServerAnalysis.activeAnalyses.keySet())
            StaticServerAnalysis.activeAnalyses.put(a, false);
    }

    @FunctionalInterface
    private interface EvalMethod {
        double eval(Collection<? extends Module> files, AnalysisConsumer consumer);
    }
}
