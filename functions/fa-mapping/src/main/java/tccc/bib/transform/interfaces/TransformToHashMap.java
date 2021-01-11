package tccc.bib.transform.interfaces;

import com.microsoft.azure.functions.ExecutionContext;

import java.util.AbstractMap;

public interface TransformToHashMap {
    public Object transform(String payload, AbstractMap<String, Object> properties, final ExecutionContext context )
            throws Exception ;
}
