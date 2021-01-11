package tccc.bib.transform.interfaces;

import com.microsoft.azure.functions.ExecutionContext;

import java.util.AbstractMap;

public interface TransformFromHashMap {
    public String transform(Object payload, AbstractMap<String, Object> properties, final ExecutionContext context )
            throws Exception ;
}
