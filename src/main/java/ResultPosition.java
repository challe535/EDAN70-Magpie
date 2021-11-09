import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IMethod.SourcePosition;

import org.extendj.ast.ASTNode;

import beaver.Symbol;

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

        firstLine = Symbol.getLine(firstOffset);
        lastLine = Symbol.getLine(lastOffset);

        firstCol = Symbol.getColumn(firstOffset);
        lastCol = Symbol.getColumn(lastOffset);

        String sourceFilePath = cuPath;

        try {
            urlToSourceFile = new URL("file://" + sourceFilePath);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public ResultPosition(int firstLine, int lastLine,int firstCol, int lastCol, String cuPath) {
        this.firstOffset = Symbol.makePosition(firstLine, firstCol);
        this.lastOffset = Symbol.makePosition(lastLine, lastCol);

        this.firstLine = firstLine;
        this.lastLine = lastLine;

        this.firstCol = firstCol;
        this.lastCol = lastCol;

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
