package tccc.bib.tools;

public class IndexTracker {

    private Integer _index = 0;
    private Integer index_ = 0;

    public void incrementSource(){
        _index++;
    }

    public void incrementTarget(){
        index_++;
    }

    public Integer getSource(){
        return _index;
    }

    public Integer getTarget(){
        return index_;
    }

}