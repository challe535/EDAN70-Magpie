import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IMethod.SourcePosition;

import org.extendj.ast.ASTNode;

public class ResultPosition implements Position {
    private int firstOffset;
    private int lastOffset;
    private int firstLine;
    private int lastLine;
    private int firstCol;
    private int lastCol;

    private URL urlToSourceFile;

    //Default constructor usefull for testing
    public ResultPosition(String cuPath) {
        firstOffset = 3;
        lastOffset = 10;

        firstLine = 1;
        lastLine = 1;

        firstCol = 3;
        lastCol = 10;

        String sourceFilePath = cuPath;

        try {
            urlToSourceFile = new URL("file://" + sourceFilePath);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public ResultPosition(ASTNode resultSourceNode, String cuPath) {
        firstOffset = resultSourceNode.getStart();
        lastOffset = resultSourceNode.getEnd();

        firstLine = resultSourceNode.lineStart();
        lastLine = resultSourceNode.lineEnd();

        firstCol = resultSourceNode.columnStart();
        lastCol = resultSourceNode.columnEnd();

        String sourceFilePath = cuPath;

        try {
            urlToSourceFile = new URL("file://" + sourceFilePath);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getFirstLine() {
        return firstLine;
    }

    @Override
    public int getLastLine() {
        return lastLine;
    }

    @Override
    public int getFirstCol() {
        return firstCol;
    }

    @Override
    public int getLastCol() {
        return lastCol;
    }

    @Override
    public int getFirstOffset() {
        return firstOffset;
    }

    @Override
    public int getLastOffset() {
        return lastOffset;
    }

    @Override
    public URL getURL() {
        return urlToSourceFile;
    }

    @Override
    public int compareTo(SourcePosition o) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Reader getReader() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }
    
}
