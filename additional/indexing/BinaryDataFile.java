import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;

public abstract class BinaryDataFile<T, C extends Collection<T>, O>{
    protected RandomAccessFile randomAccessFile;

    public BinaryDataFile(File scoreIndexFile){
        if (!scoreIndexFile.exists()){
            try {
                scoreIndexFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            randomAccessFile = new RandomAccessFile(scoreIndexFile, "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    abstract void write(T element);

    abstract O writeAll(C dataStructure);

    abstract T read();

    public T read(long bytePosition){
        try {
            randomAccessFile.seek(bytePosition);
            return read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    abstract C readAll() throws IOException;

}
