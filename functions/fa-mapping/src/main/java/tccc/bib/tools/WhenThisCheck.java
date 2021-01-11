package tccc.bib.tools;

import java.util.*;
import com.microsoft.azure.functions.ExecutionContext;
import java.math.BigDecimal;
import java.util.Date;

public class WhenThisCheck {
    // publicnow
    @SuppressWarnings("unchecked")
    public static Boolean check(
        String action, 
        AbstractMap<String, Object> node, 
        AbstractList<AbstractMap<String, Object>> whenThis, 
        Object request, 
        Object loop, 
        String space,
        Object root,
        Object bookmark, 
        AbstractMap<String, Object> inboundAttributes,
        IndexTracker index,
        final ExecutionContext context)
            throws java.lang.Exception {
               // System.out.println(String.format("%sStarting whenThisCheck with action: %s and %d number of items", space, action, whenThis.size()));

        Boolean result = true;

        for(Map<String, Object> item : whenThis){

            //System.out.println(String.format("%sLooping over item list at item: %s", space, item.toString()));
            Boolean temp_result = false;

            if(((String)item.get("action")).equals("STATEMENT")){

                Object leftop = item.get("left");
                String op = (String)item.get("op");
                Object rightop = item.get("right");
                String _leftop = (String)leftop;
                String _rightop = (String)rightop;

                //println space + "Statement: " + leftop + " " + op + " " + rightop;
                String statement = String.format("%sStatement: %s %s %s", space, leftop.toString(), op, rightop.toString());
                //context.getLogger().info(String.format("%sStatement: %s %s %s", space, leftop.toString(), op, rightop.toString()));

                try {

                    if(_leftop.indexOf("$$") > -1 ||
                        _leftop.indexOf("&&") > -1 ||
                        _leftop.indexOf("%%") > -1 ||
                        _leftop.indexOf("@@") > -1){

                            leftop = GetValue.get(_leftop, request, root, bookmark, inboundAttributes, index, context);                                        // GET VALUE
                    } else if(_leftop.indexOf("!!") > -1){
                            leftop = (String)GetValue.get(_leftop.replace("!!","$$"), loop, root, bookmark, inboundAttributes, index, context);                        // GET VALUE
                    }

                    if(_rightop.indexOf("$$") > -1 ||
                            _rightop.indexOf("&&") > -1 ||
                            _rightop.indexOf("%%") > -1 ||
                            _rightop.indexOf("@@") > -1){
                        rightop = (String)GetValue.get(_rightop, request, root, bookmark, inboundAttributes, index, context);                                      // GET VALUE
                        
                    } else if(_rightop.indexOf("!!") > -1){
                        rightop = (String)GetValue.get(_rightop.replace("!!","$$"), loop, root, bookmark, inboundAttributes, index, context);                      // GET VALUE
                    }

                    if(!Objects.isNull(leftop)) {
                        LinkedHashMap<String, Object> nodel = new LinkedHashMap<>();
                        nodel.put("type","SIMPLE");
                        nodel.put("special",new LinkedList<AbstractMap<String,Object>>());
                        if(((String)item.get("type")).equals("DATETIME")){

                            AbstractMap<String, Object> datetime = new LinkedHashMap<>();
                            datetime.put("source",(String)item.get("leftDateFormat"));
                            datetime.put("target","");
                            AbstractMap<String,Object> dateConfig = new LinkedHashMap<>();
                            dateConfig.put("datetime",datetime);
                            dateConfig.put("type","datetime");
                            ((AbstractList<AbstractMap<String,Object>>)nodel
                                .get("special")).add(dateConfig);
                        }
                        leftop = Transform.transform(leftop, nodel, request, root, bookmark, inboundAttributes, index, context);
                    }

                    if(!Objects.isNull(rightop)) {
                        java.util.LinkedHashMap<String, Object> noder = new LinkedHashMap<>();
                        noder.put("type","SIMPLE");
                        noder.put("special",new LinkedList<AbstractMap<String,Object>>());
                        if(((String)item.get("type")).equals("DATETIME")){

                            AbstractMap<String, Object> datetime = new LinkedHashMap<>();
                            datetime.put("source",(String)item.get("rightDateFormat"));
                            datetime.put("target","");
                            AbstractMap<String,Object> dateConfig = new LinkedHashMap<>();
                            dateConfig.put("datetime",datetime);
                            dateConfig.put("type","datetime");
                            ((AbstractList<AbstractMap<String,Object>>)noder
                                .get("special")).add(dateConfig);
                        }

                        rightop = Transform.transform(rightop, noder, request, root, bookmark, inboundAttributes, index, context);
                    }
                    
                    //System.out.println(String.format("%sStatement: %s %s %s", space, Objects.isNull(leftop)?"NULL":leftop.toString(), op, Objects.isNull(rightop)?"NULL":rightop.toString()));

                    if(item.get("type").equals("STRING")){
                        leftop = Objects.isNull(leftop)?null:leftop.toString();
                        rightop = Objects.isNull(rightop)?null:rightop.toString();
                        switch(op){
                            case "EQUALS":
                                temp_result = (leftop.equals(rightop));
                                break;
                            case "NOT EQUALS":
                                temp_result = (!leftop.equals(rightop));
                                break;
                            case "CONTAINS":
                                temp_result = ((String)leftop).contains((String)rightop);
                                break;
                            case "DOES NOT CONTAIN":
                                temp_result = !((String)leftop).contains((String)rightop);
                                break;
                            case "IS NULL":
                                temp_result = Objects.isNull(leftop);
                                break;
                            case "IS NOT NULL":
                                temp_result = !Objects.isNull(leftop);
                                break;
                        }

                       // System.out.println(String.format("%sStatement: %s %s %s is %s", space, Objects.isNull(leftop)?"NULL":leftop.toString(), op, Objects.isNull(rightop)?"NULL":rightop.toString(),temp_result.toString()));

        
                    } else
                    if(item.get("type").equals("NUMBER")){
                                            _leftop = Objects.isNull(leftop)?null:leftop.toString().trim();
                        _rightop = Objects.isNull(rightop)?null:rightop.toString().trim();

                        switch(op){
                            case "EQUALS":
                                temp_result = (new BigDecimal(_leftop)).compareTo(new BigDecimal(_rightop)) == 0;
                                break;
                            case "NOT EQUALS":
                                temp_result = !((new BigDecimal(_leftop)).compareTo(new BigDecimal(_rightop)) == 0);
                                break;
                            case "LESS THAN":
                                temp_result = (new BigDecimal(_leftop)).compareTo(new BigDecimal(_rightop)) == -1;
                            break;
                        case "GREATER THAN":
                            temp_result = (new BigDecimal(_leftop)).compareTo(new BigDecimal(_rightop)) == 1;
                            break;
                        case "LESS THAN OR EQUAL":
                                temp_result = (new BigDecimal(_leftop)).compareTo(new BigDecimal(_rightop)) < 1;
                                break;
                        case "GREATER THAN OR EQUAL":
                                temp_result = (new BigDecimal(_leftop)).compareTo(new BigDecimal(_rightop)) > -1;
                                break;
                            case "IS NULL":
                                temp_result = Objects.isNull(leftop);
                                break;
                            case "IS NOT NULL":
                                temp_result = !Objects.isNull(leftop);
                                break;
                        }
                        
                        //System.out.println(String.format("%sStatement: %s %s %s is %s", space, Objects.isNull(leftop)?"NULL":leftop.toString(), op, Objects.isNull(rightop)?"NULL":rightop.toString(),temp_result.toString()));

                    
                    } else 
                    if(item.get("type").equals("DATETIME")){
                    
                        _leftop = Objects.isNull(leftop)?null:(new Long(((Date)leftop).getTime())).toString();
                        _rightop = Objects.isNull(rightop)?null:(new Long(((Date)rightop).getTime())).toString();

                        switch(op){
                            case "EQUALS":
                                temp_result = (new BigDecimal(_leftop)).compareTo(new BigDecimal(_rightop)) == 0;
                                break;
                            case "NOT EQUALS":
                                temp_result = !((new BigDecimal(_leftop)).compareTo(new BigDecimal(_rightop)) == 0);
                                break;
                                case "LESS THAN":
                                temp_result = (new BigDecimal(_leftop)).compareTo(new BigDecimal(_rightop)) == -1;
                                break;
                            case "GREATER THAN":
                                temp_result = (new BigDecimal(_leftop)).compareTo(new BigDecimal(_rightop)) == 1;
                                break;
                            case "LESS THAN OR EQUAL":
                                temp_result = (new BigDecimal(_leftop)).compareTo(new BigDecimal(_rightop)) < 1;
                                break;
                            case "GREATER THAN OR EQUAL":
                                temp_result = (new BigDecimal(_leftop)).compareTo(new BigDecimal(_rightop)) > -1;
                                break;
                            case "IS NULL":
                                temp_result = Objects.isNull(leftop);
                                break;
                            case "IS NOT NULL":
                                temp_result = !Objects.isNull(leftop);
                                break;
                        }
                        //System.out.println(String.format("%sStatement: %s %s %s is %s", space, Objects.isNull(leftop)?"NULL":leftop.toString(), op, Objects.isNull(rightop)?"NULL":rightop.toString(),temp_result.toString()));
                    }
                
                }
                catch (Exception ex){
                    throw new Exception(String.format("Failed at when condition of field <%s> at statement <%s> with exception: %s",Objects.isNull(node)?"unknown":(String)node.get("name"), statement, ex.toString()));
                }
            
            } 
			else {

                AbstractList<Object> list = new LinkedList<>();

                if(!item.get("loop").equals("")){

                    //println space + "Loop: " + item['loop'];
                    //System.out.println(String.format("%sLoop: %s", space, item.get("loop")));

                    Object _list = null;

                    if(((String)item.get("loop")).indexOf("$$") > -1 ||
                        ((String)item.get("loop")).indexOf("&&") > -1 ||
                        ((String)item.get("loop")).indexOf("%%") > -1 ||
                        ((String)item.get("loop")).indexOf("@@") > -1){
                        _list = GetValue.get((String)item.get("loop"), request, root, bookmark, inboundAttributes, index, context);                              // GET VALUE
                    }

                    if(((String)item.get("loop")).indexOf("!!") > -1){
                        _list = GetValue.get(((String)item.get("loop")).replace("!!","$$"), loop, root, bookmark, inboundAttributes, index, context);            // GET VALUE
                    }

                    if(!(_list instanceof AbstractList)){
                        list.add(_list);
                    }
                    else {
                        list = (AbstractList<Object>)_list;
                    }
                    //println space + "Loop size: " + list.size();
                    //System.out.println(String.format("%sLoop size: %d", space, list.size()));

                }

                if(list.size() == 0){
                    temp_result = check((String)item.get("action"), node, (AbstractList<AbstractMap<String, Object>>)item.get("nodes"),
                            request, loop, space + "   ", root, bookmark, inboundAttributes, index, context);
                    //println space + "No list result: " + temp_result;
                    //System.out.println(String.format("%sNo list result: %b", space, temp_result));
                }
                else {
                    for(Object il : list){

                        Boolean _break = false;

                        for(AbstractMap<String, Object> nd : (AbstractList<AbstractMap<String, Object>>)((AbstractMap<String, Object>)item).get("nodes")){

                            Boolean ttemp_result = null;

                            AbstractList<AbstractMap<String, Object>> llist =  new LinkedList<AbstractMap<String, Object>>();
                             llist.add(nd);

                            if(nd.get("action").equals("STATEMENT")) {
                                ttemp_result = check((String) nd.get("action"), node, llist, request, il, space + "   ", root, bookmark, inboundAttributes, index, context);
                            }
						    else {
                                ttemp_result = check((String)nd.get("action"), node, (AbstractList<AbstractMap<String, Object>>)nd.get("nodes"),
                                        request, il, space + "   ", root, bookmark, inboundAttributes, index, context);
                            }

                            
                            if(((String)item.get("action")).equals("AND") && !ttemp_result){
                                temp_result = false;
                                _break = true;
                                break;
                            }
                            if(((String)item.get("action")).equals("OR") && ttemp_result) {
                                temp_result = true;
                                _break = true;
                                break;
                            }
                            if(((String)item.get("action")).equals("NOT AND") && !ttemp_result){
                                temp_result = false;
                                _break = true;
                                break;
                            }
                            if(((String)item.get("action")).equals("NOT OR") && ttemp_result) {
                                temp_result = true;
                                _break = true;
                                break;
                            }

                            temp_result = ttemp_result;
                        }

                        if(_break)break;
                    }

                    temp_result = (((String)item.get("action")).indexOf("NOT") > -1)?!temp_result:temp_result;
                    //println space + "Loop result: " + temp_result;
                    //System.out.println(String.format("%sLoop Result: %b", space, temp_result));

                }
            }


            if(action.equals("AND") && !temp_result)	 return false;
            if(action.equals("OR") && temp_result) return true;
            if(action.equals("NOT AND") && !temp_result) return true;
            if(action.equals("NOT OR") && temp_result) return false;

            result = temp_result;
        }

        return (action.indexOf("NOT") > -1)?!result:result;


    }
}
