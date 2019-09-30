import java.util.Objects;

public class DocumentVectorPointer {
    private int docId;
    private long bytePosition;
    private int docFreq;

    public DocumentVectorPointer(int docId, long bytePosition, int docFreq) {
        this.docId = docId;
        this.bytePosition = bytePosition;
        this.docFreq = docFreq;
    }

    public int getDocId() {
        return docId;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

    public long getBytePosition() {
        return bytePosition;
    }

    public void setBytePosition(long bytePosition) {
        this.bytePosition = bytePosition;
    }

    public int getDocFreq() {
        return docFreq;
    }

    public void setDocFreq(int docFreq) {
        this.docFreq = docFreq;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentVectorPointer that = (DocumentVectorPointer) o;
        return docId == that.docId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(docId);
    }
}
