const ae3 = require("ae3");

const Server = module.exports = ae3.Class.create(
	"Server",
	require('ae3.web/Share'),
	function(){
		this.Share();
		return this;
	},
	{
		onDrill : {
			value : function(context) {
				return context.setSkin(this.skin, this.skinData);
			}
		},
		onHandle : {
			value : function(context) {
				return {
					layout	: 'message',
					code	: 500,
					title	: 'Invalid skin settings',
					text	: 'Invalid skin settings: skin=' + this.skin,
					detail	: Layouts.toObject(this.skinData)
				}
			}
		}
	}
);
