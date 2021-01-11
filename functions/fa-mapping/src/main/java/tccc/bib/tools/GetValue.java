package tccc.bib.tools;

import java.util.*;
import com.microsoft.azure.functions.ExecutionContext;


public class GetValue {
    // publicnow
    @SuppressWarnings("unchecked")
    public static Object get(
            String name, 
            Object request,
            Object root,
            Object bookmark,
            AbstractMap<String, Object> inboundAttributes, 
            IndexTracker index, 
            final ExecutionContext context){

        //context.getLogger().info(String.format("Entered getValue method: %s",name));

        String[] tokens = name.split("\\.");
        Object _request = request;
        
        for(String token : tokens){

            Integer _index = null;
            Boolean arrayFlag = false;

            if(token.indexOf('[') > -1 && (token.indexOf(']') + 1) == token.length()){
                _index = Integer.parseInt(
                        token.substring(token.indexOf('[') + 1, 
                        token.indexOf(']'))
                    );
                token = token.substring(0, token.indexOf('['));
            }

            
            if(token.equals("$$")) {
                if(_request instanceof AbstractList<?> && _index != null){
                    _request = ((AbstractList<?>)_request).get(_index);
                } 
                continue; 
            }

            if(token.equals("%%")) { 
                if(bookmark instanceof AbstractList<?> && _index != null){
                    _request = ((AbstractList<?>)bookmark).get(_index);
                    continue; 
                } 
                _request = bookmark;
                continue; 
            }
    
            if(token.equals("&&")) {
                bookmark = _request;
                
                if(root instanceof AbstractList<?> && _index != null){
                    _request = ((AbstractList<?>)root).get(_index);
                    continue; 
                } 
                _request = root; 
                continue; 
            }

            if(token.equals("index_")){
                return index.getTarget();
            }

            if(token.equals("_index")){
                return index.getSource();
            }
    

            if(token.indexOf("**")==0){
                arrayFlag = true;
                token = token.substring(2);
            }

            if(token.equals("%")){
               if(_request instanceof AbstractList){
                    LinkedList<Object> newlist = new LinkedList<>();
                            
                    for(Object item : (AbstractList<?>)_request){
                        if(!Objects.isNull(item)){
                            if(item instanceof AbstractMap){
                                item = ((AbstractMap)item).get(token);
                            }
                            newlist.add(item);
                        } 
                        else {
                            newlist.add(null);
                        }
                    }
                     
                    return newlist;
                }

                if(_request instanceof java.lang.String){
                    return _request;
                }

            }

            //context.getLogger().fine("Looping tokens: " + token);

           if(token.equals("@@")) {
               _request = inboundAttributes;
               continue;
           }

            if(_request == null) return null;

            if(_index == null){
                if(_request instanceof AbstractList){

                    LinkedList<Object> newList = new LinkedList<>();
                    AbstractList<?> temprequest = (AbstractList<?>)_request;
                    _request = null;

                    for(Object r : temprequest){

                        if(!(r instanceof AbstractMap))
                            continue;

                        if(((AbstractMap<?,?>)r).get(token) instanceof java.util.AbstractList){
                            for(Object i : (AbstractList<?>)((AbstractMap<?,?>)r).get(token)){
                                if(i instanceof AbstractList){
                                    //newList.addAll((AbstractList)i);
                                    for(Object ii : (AbstractList<?>)i){
                                        if(ii instanceof AbstractMap){
                                            ((AbstractMap<String, Object>)ii).put("^^",r);
                                            //System.out.println("Add item in list.");
                                            newList.add(ii);
                                        } 
                                    }
                                }
                                else if(i instanceof AbstractMap){
                                    //System.out.println("Add single item in list.");
                                            
                                    ((AbstractMap<String, Object>)i).put("^^",r);
                                    newList.add(i);
                                } 
                                else {
                                    newList.add(i);
                                }
                                
                            }
                        }
                        else {
                            Object i = ((Map<?,?>)r).get(token);
                            if(i instanceof AbstractList){
                                for(Object ii : (AbstractList)i){
                                    if(ii instanceof AbstractMap){
                                        ((AbstractMap<String, Object>)ii).put("^^",r);
                                        newList.add(ii);
                                    } 
                                }
                            }
                            else if(i instanceof AbstractMap){
                                ((AbstractMap<String, Object>)i).put("^^",r);
                                newList.add(i);
                            }
                            else {
                                newList.add(i);
                            }

                            
                        }
                    }
                    _request = newList;
                }
                else {
                    if(_request instanceof AbstractMap){
                        _request = ((AbstractMap<String, Object>)_request).get(token);
                        if(arrayFlag && !(_request instanceof AbstractList)){
                            AbstractList<Object> temp = new LinkedList<>();
                            temp.add(_request);
                            _request = temp;
                        }
                    }
                    else {
                        _request = null;
                    }
                }
            } 
            else {
                if(_request instanceof AbstractList<?>){
                    LinkedList<Object> newList = new LinkedList<>();
                    AbstractList<?> temprequest = (AbstractList<?>)_request;
                    _request = null;

                    for(Object r : temprequest){

                        if(((Map<?,?>)r).get(token) instanceof java.util.AbstractList){
                            newList.add( ((List<?>)((Map<?,?>)r).get(token)).get(_index));
                        }
                        else {
                            newList.add(((Map<?,?>)r).get(token));
                        }
                    }
                    _request = newList;
                }
                else {
                    _request = ((AbstractMap<String, Object>)_request).get(token);
                    if(_request instanceof AbstractList<?> ){
                        if(((AbstractList<Object>)_request).size() > _index){
                            _request = ((AbstractList<Object>)_request).get(_index);
                        }
                        else {
                            _request = null;
                        }
                    }

                        
                }
            }
        }

        //context.getLogger().fine("Leaving getValue method with a value.");
        return _request;
    }

}
