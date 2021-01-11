package tccc.bib.interfaces;

import tccc.bib.objects.Request;
import tccc.bib.objects.Response;
import com.microsoft.azure.functions.*;


public interface IRoute {
    public Response send(Request request,final ExecutionContext context) 
        throws Exception ;
}