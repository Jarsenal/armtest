//'use strict';
//angular.module('todoApp')
app.controller('mappingCtrl', function ($scope, $http, modals) {
	
	// private
	var count = 0;
	
	function refresh(){
		
		uneditTree($scope.tree);
		uneditTree($scope.treeProperty);
		    	
		let finalData = JSON.stringify(readTree($scope.tree, true));
		let finalDataProperty = JSON.stringify(readTree($scope.treeProperty, true));
		let request = JSON.parse(JSON.stringify($scope.request));
		request.payloadProperty = {};
		for(i in $scope.request.payloadProperty){
			request.payloadProperty[$scope.request.payloadProperty[i].key] = $scope.request.payloadProperty[i].value;
		}
    	request.mapping = finalData;
    	request.mappingProperty = finalDataProperty;
    	request.sourceFF = JSON.stringify($scope.sourceFF);
		request.targetFF = JSON.stringify($scope.targetFF);
		request.test = true;
    	
    	let data = JSON.stringify(request);
    	let config = {headers : {
				'Content-Type': 'application/json',
				'attributes':'{}'
				},
				params : {'code':$scope.biibKey},
    			transformResponse: [function(data) {return data;}]};
		
		$http.post($scope.apiHost + '/process/mappings', data, config).
		then(
			function(response) {
				if($scope.request.response === "JSON"){
					$scope.data = JSON.stringify(JSON.parse(response.data),null, 3);
				} 
				else {
					$scope.data = response.data;
				}

				$scope.dataAttributes = [];
				let atts = JSON.parse(response.headers()['attributes']);
				for(item in atts){
					$scope.dataAttributes.push({
						key: item,
						value: atts[item]
					})
				}

			}, 
			function(response) {
				$scope.data = response.data;
				$scope.dataAttributes = [];
			}
		);
    }
	
	function save(name){
    	
		var finalData = JSON.stringify(readTree($scope.tree, true));
		var finalDataProperty = JSON.stringify(readTree($scope.treeProperty, true));
    	var request = $scope.request;
    	request.mapping = finalData;
    	request.mappingProperty = finalDataProperty;
    	
    	var data = JSON.stringify(request);
    	var config = {headers : {
			'Content-Type': 'application/json',
			'code':$scope.biibKey
			},
			params : {'code':$scope.biibKey}
		};


		$http.post($scope.apiHost + '/configs/mappings/' + name, data, config).
		then(
			function(response) {
				if((typeof response.data) == "string")
					$scope.data = response.data;
				else
					$scope.data = JSON.stringify(response.data, null, 2);
				$scope.mapping = name;
			}, 
			function(response) {
				$scope.data = response.data;
			}
		);
    }
	
	function readTree(_node, root){
    	var node = {
    			name: _node.name,
    			value: _node.value,
    			type: _node.type,
				list: _node.list,
				script: _node.script,
    			when: _node.when?_node.when:[],
    			required: _node.required,
    			ignore: _node.ignore,
				special: _node.special,
				listspecial: _node.listspecial?_node.listspecial:[],
    			nodes: []
    	}
    	
    	// just for the root
    	if(root && _node.source != null){
    		 node.source = {}
	    	if(_node.source.header != null) node.source.header = _node.source.header
	    	if(_node.source.quotes != null) node.source.quotes = _node.source.quotes
	    	if(_node.source.delimiter != null) node.source.delimiter = _node.source.delimiter
    	}
    	if(root && _node.target != null){
    		 node.target={}
	    	if(_node.target.header != null) node.target.header = _node.target.header
	    	if(_node.target.quotes != null) node.target.quotes = _node.target.quotes
	    	if(_node.target.delimiter != null) node.target.delimiter = _node.target.delimiter
    	}
    	
    	if(_node.type === "OBJECT" || _node.type === "CUSTOM"){
	    	for(index in _node.nodes){
	    		node.nodes.push(readTree(_node.nodes[index], false));
	    	}
    	}
    	
    	return node;
    }
    
    
    
    function uneditTree(_node){
    	_node.edit = false;
    	for(index in _node.nodes){
    		uneditTree(_node.nodes[index])
    	}
    	
    	$scope.node.edit = null;
    }
    
    function newNode(_parent){
    	count += 1;
        var newName = "NewField" + count;
        return {
    	 id: count,
		 name: newName,
		 when: [],
		 type: "SIMPLE",
		 value: "",
		 script: "",
     	 parent: _parent,
     	 list: false,
     	 required: false,
     	 ignore: false,
		 edit: false,
     	 shownodes: true,
		 special: [],
		 listspecial: [],
     	 nodes: []
	   };
    }
    
    
    
    function buildNode(_parent, _node){
    	count += 1;
        var new_node = {
    	 id: count,
		 name: _node.name,
		 type: _node.type,
		 value: _node.value,
		 when: _node.when?_node.when:[],
		 parent: _parent,
		 script: _node.script,
     	 list: _node.list,
	     required: _node.required,
	     ignore: _node.ignore,
		 special: _node.special,
		 listspecial: _node.listspecial?_node.listspecial:[],
     	 edit: false,
     	shownodes: true,
     	 nodes: []
	   };
        
        // just for the root
        if(_node.source){
        	new_node.source = _node.source;
        }
        if(_node.target){
        	new_node.target = _node.target;
        }
    	
	   
	   if(_node.nodes && _node.nodes.length > 0){
	    	for(index in _node.nodes){
	    		new_node.nodes.push(buildNode(new_node, _node.nodes[index]));
	    	}
	   }
	   return new_node;
   	}
   	
    function buildTree(_node){
    	var tree = buildNode(null, _node);
    	return tree;
    }
    
    
    // public
	
    // models
    $scope.data = "";
    $scope.dataAttributes = [];
    $scope.tree = {
    			name: "root", 
    			type: "OBJECT", 
    			list: false,
    			when: [],
    			source: {
	    			header: false,
	    			quotes: false,
	    			delimiter: ","
    			},
    			target: {
	    			header: false,
	    			quotes: false,
	    			delimiter: ","
    			},
				special: [],
				listspecial: [],
    			nodes:[]
    };
    
    $scope.treeProperty = {
    		type: "OBJECT",
    		list: false,
			nodes:[]
    };

    
    $scope.request = {
    		payload: "{}",
    		response: "JSON",
    		type: "JSON",
    		payloadProperty: []
    };
    
    $scope.sourceFF = {
			newLine: '\n',
			schemas: []
		}
    
    $scope.targetFF =   {
			newLine: '\n',
			schemas: []
		}
    
    $scope.mapping="New-Mapping";
    $scope.node = {edit:null};

    //    which to view
    $scope.toggle = {
    		source: "PAYLOAD",
    		target: "PAYLOAD",
    		mapping: "PAYLOAD"
    }
     
    // type 
    $scope.typeSwitch = function(){
    	
    	if($scope.toggle.source == "PAYLOAD"){
    		
    	} 
    	else if($scope.toggle.source == "ATTRIBUTES") {
    		
    	}
    	
    	
    	refresh();
    }
    
    // request text
    $scope.sourceChange = function(){
    	refresh();
    }
    
    // tree controls
	$scope.deleteNode = function(data){
		_index = -1;
		if(data.parent != null)
		{
			for(index in data.parent.nodes){if(data.parent.nodes[index].id == data.id){_index = index;}}
				if(_index > -1){data.parent.nodes.splice(_index,1);}
		}
		refresh();
	}
	
	$scope.addChild = function(data) {
		data.nodes.push(newNode(data));
        refresh();
    };
    
    $scope.editNode = function(data){
    	uneditTree($scope.tree);
    	uneditTree($scope.treeProperty);
    	data.edit = true;
    	$scope.node.edit = data;
    }
    
    $scope.saveNode = function(data){
    	data.edit = false;
		data.special = $scope.node.edit.special;
		data.listspecial = $scope.node.edit.listspecial;
    	refresh();
    }
    
    $scope.updateNodeType = function(data){
    	if(data.type != "OBJECT" && data.type != "CUSTOM"){data.nodes=[];}
    	if(data.type == "OBJECT" || data.type == "CUSTOM"){data.value="";}
    }
    
    // mapping load/save
    $scope.loadMapping = function() {
        var promise = modals.open(
            "load",
            {}
        );
        promise.then(
            function handleResolve( response ) {
                console.log( "Prompt resolved with [ %s ].", response );
				var mymapping = response;
				
				var config = {params : {'code':$scope.biibKey}};
		

				$http.get($scope.apiHost + '/configs/mappings/' + response, config).
				then(
        			function(response) {
        				$scope.request.type = response.data.type;
        				$scope.request.response = response.data.response;
        				$scope.request.payload = response.data.payload;
        				if(response.data.payloadProperty)
        					$scope.request.payloadProperty = response.data.payloadProperty;
        				if(response.data.sourceFF)
        					$scope.sourceFF = JSON.parse(response.data.sourceFF);
        				if(response.data.targetFF)
        					$scope.targetFF = JSON.parse(response.data.targetFF);
        				
        				if($scope.request.payloadProperty === undefined || $scope.request.payloadProperty === null) 
        					$scope.request.payloadProperty = [];
        				
        				$scope.mapping = mymapping;
        				var mapping = JSON.parse(response.data.mapping);
        				
        				var mappingProperty = {type: "OBJECT", nodes:[]}; 
        				if(response.data.mappingProperty)
        					mappingProperty = JSON.parse(response.data.mappingProperty);
        				
        				$scope.tree = buildTree(mapping);
        				$scope.treeProperty = buildTree(mappingProperty);
        				 
        				refresh();
        				
        			}, 
        			function(response) {
        				alert("There was an error loading the mapping");
        			}
        		);
                
                
            },
            function handleReject( error ) {
                console.warn( "Prompt rejected!" );
            }
        );
    };

    $scope.saveMapping = function() {
    	
    	if($scope.mapping==="New-Mapping"){
	        var promise = modals.open(
	            "save",
	            {mapping: $scope.mapping}
	        );
	        promise.then(
	            function handleResolve( response ) {
	                console.log( "Save map to [ %s ].", response );
	                save(response);
	            },
	            function handleReject( error ) {
	                console.warn( "Prompt rejected!" );
	            }
	        );
    	}
    	else
		{
    		save($scope.mapping);
		}
    };
    
    $scope.saveasMapping = function() {
    	
        var promise = modals.open(
            "save",
            {mapping: $scope.mapping}
        );
        promise.then(
            function handleResolve( response ) {
                console.log( "Save map to [ %s ].", response );
                save(response);
            },
            function handleReject( error ) {
                console.warn( "Prompt rejected!" );
            }
        );

    }
    
    $scope.generateMapping = function() {
    	
        var promise = modals.open(
            "generator",
            {}
        );
        promise.then(
            function handleResolve( response ) {
                console.log( "Save map to [ %s ].", response );
                var data = response;
                
                var config = {};
                if($scope.request.response == "JSON")
				config = {headers : {'Content-Type': 'application/json'},
				params : {'code':$scope.biibKey}};
                
                if($scope.request.response == "XML")
				config = {headers : {'Content-Type': 'application/xml'},
							params : {'code':$scope.biibKey}};
                
        		
        		$http.post('map-api/v1/mappings/generate', data, config).
        		then(
 
        			function(response) {
        				$scope.tree = buildTree(response.data);
        				refresh();
        			}, 
        			function(response) {
        				alert(response);
        			}
        		);
                
                
            },
            function handleReject( error ) {
                console.warn( "Prompt rejected!" );
            }
        );

    }

    
     // advance settings;
	$scope.advancepick = "";
    
    $scope.addXREF = function(node){
    	if(node.xref == null)
    		node.xref=[];
    	
    	node.xref.push(
    			{
    				key:"",
    				value:""
    			});
    }
    
    $scope.removeXREF = function(index, node){
    	if(node.uniquefilter == null)
    		return;
    	
    	node.uniquefilter.splice(index,1);
    	
    	if(node.uniquefilter.length == 0)
    		node.uniquefilter = null;
    	
	}
	
	$scope.addUniqueFilter = function(node){
    	if(node.uniquefilter == null)
    		node.uniquefilter=[];
    	
    	node.uniquefilter.push({
			value: name
		});
    }
    
    $scope.removeUniqueFilter = function(index, node){
    	if(node.uniquefilter == null)
    		return;
    	
    	node.uniquefilter.splice(index,1);
    	
    	if(node.uniquefilter.length == 0)
    		node.uniquefilter = null;
    	
    }
    
    $scope.addAdvanceFunction = function(node, name){
    	node.special.push({
			type: name
		});
    }
        
    $scope.removeAdvanceFunction = function(index, list){
    	list.splice(index,1);
	}
	
	$scope.addListAdvanceFunction = function(node, name){
    	node.listspecial.push({
			type: name
		});
    }
        
    $scope.removeListAdvanceFunction = function(index, list){
    	list.splice(index,1);
	}
	
    
    function getIndex(parent, node){
    	_index = -1;
    	if(parent != null){
    		for(index in parent.nodes){
				if(parent.nodes[index].id == node.id){
					_index = index;
				}
			}
    	}
    	return _index;
    }
    
    $scope.nodeMoveOut = function() {
    	if($scope.node.edit != null){
    		var data = $scope.node.edit
    		var parent = $scope.node.edit.parent;
    		
    		if(parent != null && parent.parent != null){
    			var index = getIndex(parent, data);
    			if(index > -1){parent.nodes.splice(index,1);}
    			index = getIndex(parent.parent, parent);
    			data.parent = parent.parent;
    			parent.parent.nodes.splice(index,0,data);
    		}
    	}
    	refresh();
    	$scope.node.edit = data;
    	data.edit = true;
    }
    
	$scope.nodeMoveUp = function() {
		if($scope.node.edit != null){
			var data = $scope.node.edit
    		var parent = $scope.node.edit.parent;
    		
    		if(parent != null){
    			var index = Number(getIndex(parent, data));
    			if(parent.nodes[index - 1] != null){
    				var temp =  parent.nodes[index - 1];
    				parent.nodes[index - 1] = parent.nodes[index];
    				parent.nodes[index] = temp;
    			}
    		}
    	}
    	refresh();
    	$scope.node.edit = data;
    	data.edit = true;	
    }
	    
	$scope.nodeMoveDown = function() {
		if($scope.node.edit != null){
			var data = $scope.node.edit
    		var parent = $scope.node.edit.parent;
    		
    		if(parent != null){
    			var index = Number(getIndex(parent, data));
    			if(parent.nodes[index + 1] != null){
    				var temp =  parent.nodes[index + 1];
    				parent.nodes[index + 1] = parent.nodes[index];
    				parent.nodes[index] = temp;
    			}
    		}
    	}
    	refresh();
    	$scope.node.edit = data;
    	data.edit = true;
	}
	
	$scope.nodeMoveIn = function() {
		if($scope.node.edit != null){
    		var data = $scope.node.edit
    		var parent = $scope.node.edit.parent;
    		
    		if(parent != null){
    			var index = Number(getIndex(parent, data));
    			if(index > -1){
    				if(parent.nodes[index + 1] != null){
	    				if(parent.nodes[index + 1].type == "OBJECT"){
	    					data.parent = parent.nodes[index + 1];
	    	    			parent.nodes.splice(index,1);
	    	    			parent.nodes[index].nodes.splice(0,0,data);
	    				}
	    			}
    			}
    		}
    	}
    	refresh();
    	$scope.node.edit = data;
    	data.edit = true;
	}
    
	
	$scope.addPropertyItem = function(){
		$scope.request.payloadProperty.push(
					{
						key: "",
						value: ""
					}
				);
	}
    
    $scope.removePropertyItem = function(index){
    	$scope.request.payloadProperty.splice(index,1);
    }
    
    $scope.editSchema = function(schema){
    	var promise = modals.open(
                "ffschemalarge",
                {schema: schema.schemas}
            );
            promise.then(
                function handleResolve( response ) {
                	schema = response;
                },
                function handleReject( error ) {
                    console.warn( "Prompt rejected!" );
                }
            );
    }
	
	$scope.editFunctionCondition = function(item){
		if(item.getfirst == null)
			item.getfirst={
				when:[]
			};
		$scope.editCondition(item.getfirst);
	}

    $scope.editCondition = function(_node){
		
		if(!_node.when)
			_node.when=[];
		
    	var promise = modals.open(
                "whenlarge",
                {nodes: _node.when}
            );
            promise.then(
                function handleResolve( response ) {
                	_node.when = response;
                },
                function handleReject( error ) {
                    console.warn( "Prompt rejected!" );
                }
            );
    }
    
    // calls
    refresh();
});

app.controller(
        "loadMappingGenerator",
        function( $scope, $http, modals ) {
        	
        	$scope.payload = "";
            $scope.errorMessage = null;
            $scope.cancel = modals.reject;
            
            $scope.submit = function() {
                if ($scope.payload == null || $scope.payload == "" ) {
                    return( $scope.errorMessage = "Please provide something!" );
                }
                modals.resolve( $scope.payload );
            };
            
        }
    );

app.controller(
        "loadMappingController",
        function( $scope, $http, modals ) {
        	
        	$scope.mappings = [];
            $scope.mapping = {name: ""};
            $scope.errorMessage = null;
            $scope.cancel = modals.reject;
            $scope.submit = function() {
                if ($scope.mapping.name == null || $scope.mapping.name == "" ) {
                    return( $scope.errorMessage = "Please provide something!" );
                }
                modals.resolve( $scope.mapping.name );
            };
            
            
			// load dropdownlist
			var config = {params : {'code':$scope.biibKey}};
        		
			$http.get($scope.apiHost + '/configs/events/',config).
			then(
				function(response) {
					$scope.mappings = [];
					for(item in response.data){$scope.mappings.push(response.data[item].name);}
				}, 
				function(response) {
					alert("There was an error loading the events");
				}
			);
        }
    );



app.controller(
    "saveMappingController",
    function( $scope, $http, modals ) {
    	$scope.mappings = [];
    	$scope.mapping = {name: modals.params().mapping};
        $scope.errorMessage = null;
        $scope.cancel = modals.reject;
        $scope.submit = function() {
        	
            if ($scope.mapping.name == null || $scope.mapping.name == "" ) {
                return( $scope.errorMessage = "Please provide something!" );
            }
            modals.resolve( $scope.mapping.name );
        };
        
        // load dropdownlist
			var config = {params : {'code':$scope.biibKey}};
        		
			$http.get($scope.apiHost + '/configs/events/',config).
			then(
				function(response) {
					$scope.mappings = [];
					for(item in response.data){$scope.mappings.push(response.data[item].name);}
				}, 
				function(response) {
					alert("There was an error loading the events");
				}
			);
        
    }
);


app.controller(
	    "editFFSchemaController",
	    function( $scope, $http, modals ) {
	    	
	    	$scope.node = {
	    			nodes: modals.params().schema
	    	}
	    	
	    	
	    	$scope.errorMessage = null;
	        $scope.cancel = modals.reject;
	        
	        $scope.save = function() {
	            modals.resolve( $scope.data );
	        };
	        
	        $scope.addChild = function(fields) {
	        	fields.push({
	        			name:"",
	        			trim: true,
	        			length:"0",
	        			padding:" ",
	        			placement:"LEFT"})
	        };
	        
	        $scope.removeChild = function(fields, index) {
	        	fields.splice(index,1);
	        }
	        
	        $scope.addRecord = function(nodes){
	        	nodes.push({
	    			name: 'Schema',
					key: {
	    				enable: false,
	    				value: ''
	    			},
	    			fields: [],
	    			nodes: []
	    		})
	        }
	        
	        $scope.removeRecord = function(nodes, index) {
	        	nodes.splice(index,1);
	        }
	    }
	);

app.controller(
	    "editWhenController",
	    function( $scope, $http, modals ) {
	    	
	    	$scope.node = {
	    			nodes: modals.params().nodes
	    	}
	    	
	    	
	    	$scope.errorMessage = null;
	        $scope.cancel = modals.reject;
	        
	        $scope.save = function() {
	            modals.resolve( $scope.node.nodes );
	        };
	        
	        $scope.addChild = function(nodes) {
	        	nodes.push({
	        		action:"STATEMENT",
        			loop:"",
        			left: "",
        			type: "STRING",
        			op: "EQUALS",
        			right: "",
        			nodes: []
	        	})
	        };
	        
	        $scope.removeChild = function(nodes, index) {
	        	nodes.splice(index,1);
	        }
	        
	        $scope.addRecord = function(nodes){
	        	nodes.push({
	        		action:"STATEMENT",
        			loop:"",
        			left: "",
        			type: "STRING",
        			leftDateFormat: "",
        			op: "EQUALS",
        			right: "",
        			rightDateFormat: "",
        			nodes: []
	        	})
	        }
	        
	        $scope.removeRecord = function(nodes, index) {
	        	nodes.splice(index,1);
	        }
	    }
	);
