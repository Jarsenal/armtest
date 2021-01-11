package tccc.bib.methods.interfaces;

import com.microsoft.azure.functions.ExecutionContext;

import tccc.bib.tools.IndexTracker;

import java.util.AbstractMap;

public interface Method {
    public Object run(Object value, Object request, Object root, Object bookmark, AbstractMap<String, Object> inboundAttributes, AbstractMap<String, Object> config, IndexTracker index, final ExecutionContext context )
            throws Exception ;
}