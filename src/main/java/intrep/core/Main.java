package intrep.core;

import com.google.common.base.Supplier;

import org.eclipse.lsp4j.jsonrpc.messages.Either;

import intrep.analysis.AnalysisInjector;
import magpiebridge.core.IProjectService;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerAnalysis;
import magpiebridge.core.ServerConfiguration;
import magpiebridge.core.ToolAnalysis;
import magpiebridge.projectservice.java.JavaProjectService;


public class Main {

  private static ServerAnalysis myAnalysis = new StaticServerAnalysis();

  public static void main(String... args) {
    AnalysisInjector.initAnalysis();
    createServer().launchOnStdio();
  }

  private static MagpieServer createServer() {
    ServerConfiguration config = new ServerConfiguration();

    //setup server config for analysis triggering
    config.setDoAnalysisBySave(true);

    MagpieServer server = new MagpieServer(config);
    String language = "java";
    IProjectService javaProjectService = new JavaProjectService();
    server.addProjectService(language, javaProjectService);
    Either<ServerAnalysis, ToolAnalysis> analysis = Either.forLeft(myAnalysis);
    server.addAnalysis(analysis, language);

    //Test for running on self
    Boolean a = false;

    return server;
  }
}