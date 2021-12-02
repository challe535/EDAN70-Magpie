package intrep;

import java.util.Optional;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import magpiebridge.core.Kind;
import magpiebridge.core.IProjectService;
import magpiebridge.core.MagpieServer;
import magpiebridge.core.ServerConfiguration;

public class ResultsTest {
    
    @Test
    public void Result01() {
        Kind k = Kind.Diagnostic;
        MagpieServer server = new MagpieServer(new ServerConfiguration());
        Optional<IProjectService> opt = server.getProjectService("java");
        assertEquals("Example test failed", 1, 1);
    }
}