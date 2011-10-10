<?php
class CShoppingCart extends MY_Controller {

	/**
	 *
	 * Enter description here ...
	 */
	public function __construct(){
		parent::__construct();
		log_message('debug', "** CContact.[init]");

	}

	/**
	 * Index Page for this controller.
	 *
	 * Maps to the following URL
	 * 		http://example.com/index.php/welcome
	 *	- or -
	 * 		http://example.com/index.php/welcome/index
	 *	- or -
	 * Since this controller is set as the default controller in
	 * config/routes.php, it's displayed at http://example.com/
	 *
	 * So any other public methods not prefixed with an underscore will
	 * map to /index.php/welcome/<method_name>
	 * @see http://codeigniter.com/user_guide/general/urls.html
	 */
	public function index()
	{
		log_message('debug', "** CShoppingCart.index()");

		$this->load->model('Item_model');
			
		$data = array();

		// get the oferta item
		$data['ItemOferta'] = $this->Item_model->getItem('?');

		// get the special item
		$wItemSpecial = $this->Item_model->getItem('screen012');
		$data['ItemSpecial'] =$this->injectStockInItem($wItemSpecial);

		// get the new item
		$wItemNew = $this->Item_model->getItem('mouse004');
		$data['ItemNew'] =$this->injectStockInItem($wItemNew);
		
		// put in the data array given to the view the message available in the session data
		$data['CartMessage'] =$this->pSessionData->getCartMessage();
		//remove the message
		$this->pSessionData->setCartMessage('');

		$this->load->view('CShoppingCartView',$data);
	}

	/**
	 *
	 * Enter description here ...
	 */
	public function updateCart(){
		log_message('debug', "** CShoppingCart.updateCart() : _POST=[". var_export($_POST,true)."]" );

		/*
			_POST=[array (
				1 =>
					array (
						'rowid' => 'b42fbe38a2b133415908c505b8261f58',
						'qty' => '56',
					),
				2 =>
					array (
						'rowid' => '6bef792248621c2d6cd0b7321cf0af76',
						'qty' => '22',
					),
			)]
		*/
		$this->cart->update($_POST);

		$this->index();
	}

	/**
	 *
	 * Enter description here ...
	 */
	public function eraseCart(){
		log_message('debug', "** CShoppingCart.eraseCart()");

		//Permits you to destroy the cart. This function will likely be called when you are finished processing the customer's order.
		$this->cart->destroy();

		$this->index();
	}

	/**
	 *
	 * Enter description here ...
	 */
	public function applyCart(){
		log_message('debug', "** CShoppingCart.applyCart() : Cart=[". var_export($this->cart->contents(),true)."]" );
		
		

		
		
		$this->load->model('Cart_model');
		
		/*
			Cart=[array (
			  '7142441163b7d49b1efc67f9491af996' => 
				  array (
				    'rowid' => '7142441163b7d49b1efc67f9491af996',
				    'id' => 'screen011',
				    'qty' => '1',
				    'price' => '179.00',
				    'name' => 'XX Hyundai - T236LD - Ecran LED',
				    'subtotal' => 179,
				  ),
			  'a5f646a4c959b70cf25870835c5e6460' => 
				  array (
				    'rowid' => 'a5f646a4c959b70cf25870835c5e6460',
				    'id' => 'screen016',
				    'qty' => '1',
				    'price' => '281.00',
				    'name' => 'XX LG - E2290V-SN - Ecran LED',
				    'subtotal' => 281,
				  ),
			)]
		 */
		
		

		
		
		$wCartLines = array();
		
		// @see the java class org.psem2m.demo.erp.api.beans.CCartLine
		foreach ($this->cart->contents() as $wRowId=>$wRowData) {
			$wCartLine = array();
			$wCartLine['lineId'] = $wRowId;
			$wCartLine['itemId'] = $wRowData['id'];
			$wCartLine['quantity'] = $wRowData['qty'];
			$wCartLine['javaClass'] = 'org.psem2m.demo.erp.api.beans.CCartLine';
				
			array_push($wCartLines,$wCartLine);
		}
		
		// We now need to create a unique identifier for the cart.
		$wCartId = md5(microtime(false) );
		
		$wCart = array();
		$wCart['cartId']= $wCartId;
		$wCart['cartLines']= $wCartLines;
		
		//log_message('INFO', "** CShoppingCart.applyCart() : wCartLines=[". var_export($wCartLines,true)."]" );
		
		
		$wErpResponse = $this->Cart_model->applyCart($wCart);
		
		if(log_isOn('INFO')){
			log_message('INFO', "** CShoppingCart.applyCart() : wErpResponse=[". var_export($wErpResponse,true)."]" );
		}
		
		// put the message in the session data usable in the index method of the contoller
		$this->pSessionData->setCartMessage(var_export($wErpResponse,true));
		
		$this->index();
	}

}