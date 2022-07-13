
import java.util.* ;
import java.nio.file.*;

public class Pipeline {
	static ArrayList <String> registerFile = new ArrayList<>();
	public static ArrayList<Instruction> instructionMemory = new ArrayList<>();
	public static ArrayList<String> instructionMemory1 = new ArrayList<>();
	public static ArrayList <String> dataMemory = new ArrayList<>() ;
	public static ArrayList <R_Instruction> R_ID_EXE = new ArrayList<>() ; //list of r-instructions
	public static ArrayList <String> R_EXE_WB = new ArrayList<>() ;			//list of (Strings)  value we need too pass from EXE to WB (for r-instructions)
	public static ArrayList <I_Instruction> I_ID_EXE = new ArrayList<>() ; //list of i-instructions
	public static ArrayList <Integer> I_EXE_MEM = new ArrayList<>() ;	//list of (Integer) value we need too pass from EXE to MEM (for i-instructions) index
	public static ArrayList <String> I_MEM_WB = new ArrayList<>() ;		//list of (Strings) values for load (from mem to wb)	
	public static Instruction nop = new Instruction();
	public static ArrayList<String> rds = new ArrayList<>();
	public static ArrayList<String> rts = new ArrayList<>();
	public static ArrayList<String> rss = new ArrayList<>();
	public static int pc = 0;
	public static int rank = 0;
	
	public static void createInstructionSet(){
		/*
		instructionMemory1.add("00000010010100111000100000101010"); //stl s1,s2,s3
		*/
		
		//instructionMemory1.add("00001000000000000000000000000011"); //j	 3 
		//instructionMemory1.add("00010010010100100000000000000011") ; //beq s2,s2,3 
		instructionMemory1.add("00000010010100111010100000100000"); //add s5,s2,s3
		instructionMemory1.add("10101110010110000000000000000101"); //sw s8,5(s2)
		instructionMemory1.add("00000010010100111001100000100101");//or s3,s2,s3
		instructionMemory1.add("10001110100101110000000000001010");//lw s7,10(s4)
		//instructionMemory1.add("10101110010110000000000000000101"); //sw s8,5(s2)
	
		//instructionMemory1.add("00000010010100111010100000100000"); //add s5,s2,s3	
		//instructionMemory1.add("00000010101100111001100000100101");//or s3,s5,s3
	
	}
	public static void createArrs(){
		for(int n=0;n<256;n++){
			R_ID_EXE.add(null);
			R_EXE_WB.add(null);
			I_ID_EXE.add(null);
			I_EXE_MEM.add(null);
			I_MEM_WB.add(null);
			rds.add("0") ;
			rss.add("0") ;
			rts.add("0");
		}
	}
//--------------------------------------------------------------------------------------------------------------	
	public static void main(String[] args){
		nop.type = null ;
		nop.value = null ;
		createRegFile();
		//createMem();		
		try{
			createMemFromFile();	
		}catch(Exception e){
			System.out.println("Memory read  failed !");
			return ;
		}
		createArrs() ;
		createInstructionSet();			
		System.out.println("s5 value : " + registerFile.get(21));
		System.out.println("s3 value : " + registerFile.get(19));
		System.out.println("s7 value : " + registerFile.get(23));
		System.out.println("Memory[31] = " + dataMemory.get(31));
		
		System.out.println("AFTER");
		//--------------------------------------------------------------------------------------------------
			while( pc-5 <= instructionMemory1.size()){
				//System.out.println("i");
				IF(pc) ;
				if( pc-2 < instructionMemory1.size() && rank-2 >=0 ){
					ID(instructionMemory.get(pc-2)) ;	
				}
				if( pc-3 < instructionMemory1.size() && rank-3 >=0 ){
					EXE(instructionMemory.get(pc-3));	
				}
				//MEM
				if( pc-4 < instructionMemory1.size() && rank-4 >=0 ){
					if(instructionMemory.get(pc-4).type.equals("R")){
						//nothing		
					}
					if(instructionMemory.get(pc-4).type.equals("I")){
						Mem( I_ID_EXE.get(pc-4) , I_EXE_MEM.get(pc-4));
					}	
				}
				//WB
				if( pc-5 < instructionMemory1.size() && rank-5 >=0 ){
					if(instructionMemory.get(pc-5).type.equals("R")){
						rWB(R_ID_EXE.get(pc-5) , R_EXE_WB.get(pc-5)) ;
					}
					if(instructionMemory.get(pc-5).type.equals("I")){
						System.out.println(pc);
						if (I_ID_EXE.get(pc-5).mode.equals("LW")){
							//if lw
							iWB( I_ID_EXE.get(pc-5) , I_MEM_WB.get(pc-5)) ;	
						}
					}
				}

			}
		//---------------------------------------------------------------------------------------------------------------
			System.out.println("s5 value : " + registerFile.get(21));
			System.out.println("s3 value : " + registerFile.get(19));
			System.out.println("s7 value : " + registerFile.get(23));
			System.out.println("Memory[31] = " + dataMemory.get(31));
			
			
			try{
				syncMem();
			}catch(Exception e){
				System.out.println("Couldn't sync Memory ");
			}
			
			exHazardDetection();
			memHazardDetection() ;
			
	}
//---------------------------------------------------------------------------------------------------------Fetch
	public static void IF(int counter){
		if(counter >= instructionMemory1.size()){
			pc++;
			rank++ ;
			return ;
		}
		Instruction instruction = new Instruction() ;
	    instruction.value = instructionMemory1.get(counter) ; // instructionMemory1  un array ast k az jense Stringe 32tAEie 
	    instructionMemory.add(counter , instruction); // instructionMwmoery az jense instruction e 
	    pc ++ ;
	    rank ++ ;
	    return ;		    
}
//---------------------------------------------------------------------------------------------------------Decode
	public static void ID(Instruction instruction){
		String opCode = instruction.value.substring(0,6);
		if( pc-2 >= instructionMemory1.size()){
			return ;
		}
		//R-Type
		if(opCode.equals("000000")){
			instruction.type = "R" ;
			R_Instruction ins = rDecoder(instruction) ;  //Stage 2
			ins.value = instruction.value ;
			R_ID_EXE.add(pc-2,ins) ;
			return ;
		}
		
		//I-Type    branch    lw   sw
		if(!(opCode.equals("000010")||
		opCode.equals("000011") ||
		opCode.equals("010000") ||
		opCode.equals("010001") ||
		opCode.equals("010011") ||
		opCode.equals("010010") ||
		opCode.equals("111111"))){
			instruction.type = "I" ;
			I_Instruction ins = iDecodeer(instruction); //Stage 2
			ins.value = instruction.value ;
			I_ID_EXE.add(pc-2 , ins);
			return ;
		}
				
		//J-Type
		if(opCode.equals("000010") || 
		opCode.equals("000011")){
			instruction.type = "J" ; 
			jDecoder(instruction);
		}
	}
//---------------------------------------------------------------------------------------------------------EXE
	public static void EXE(Instruction instruction){
		if( pc-3 >= instructionMemory1.size()){
			return ;
		}
		if(instruction.type.equals("R")){
			 R_EXE_WB.add( pc-3 , rExecute(R_ID_EXE.get(pc-3)) );
		}
		if(instruction.type.equals("I")){
			I_EXE_MEM.add(pc-3 , iExecute(I_ID_EXE.get(pc-3)) );
		}
	}
//---------------------------------------------------------------------------------------------------------R-Decode
	public static R_Instruction rDecoder(Instruction instruction){
		String opCode = instruction.value.substring(0,6);
		String rs = instruction.value.substring(6,11);
		String rt = instruction.value.substring(11,16);
		String rd = instruction.value.substring(16,21);
		
		rss.set(pc-2 , rs); rts.set(pc-2 , rt) ; rds.set(pc-2 , rd);
		
		String shamt = instruction.value.substring(21,26);
		String func = instruction.value.substring(26,32);
		//in this part we will calculate reg numbers to reach them (decimal)
		int rsNo = Integer.parseInt(rs,2);
		int rtNo = Integer.parseInt(rt, 2);
		int rdNo = Integer.parseInt(rd, 2);
		int shI = Integer.parseInt(shamt, 2);
		
		R_Instruction ins = new R_Instruction();
		ins.first = Integer.parseInt(registerFile.get(rsNo),2);
		ins.second = Integer.parseInt(registerFile.get(rtNo),2);
		ins.func = func ;
		ins.rdNo = rdNo ;
		return ins;				
	}	
//---------------------------------------------------------------------------------------------------------I-Decode
	public static I_Instruction iDecodeer (Instruction instruction){
		String opCode = instruction.value.substring(0,6);
		String rs = instruction.value.substring(6,11);
		String rt = instruction.value.substring(11,16);
		String offset = instruction.value.substring(16,32);
		
		rss.set(pc-2 , rs); rts.set(pc-2 , rt) ; rds.set(pc-2 , "0");
		
		//in this part we will find register numbers to reach them
		int rsNo = Integer.parseInt(rs,2);
		int rtNo = Integer.parseInt(rt,2);
		int offsetNo = Integer.parseInt(offset,2);
		
		
		I_Instruction ins = new I_Instruction();
		//rs is the one wich works with memory
		ins.rsNo = rsNo ;
		ins.rsValue = registerFile.get(rsNo)   ; // rs register value in binaryString
		ins.rtNo = rtNo ;
		ins.rtValue = registerFile.get(rtNo)  ; //rt register value in binaryString (rt is what we want to save)
		ins.opCode = opCode ;
		ins.offset = offsetNo ;
		return ins ;
	}
//------------------------------------------------------------------------------------------------
		public static void jDecoder(Instruction instruction){
			String opCode = instruction.value.substring(0,6);
			String address = instruction.value.substring(6,32);
			
			rss.set(pc-2 , "0"); rts.set(pc-2 , "0") ; rds.set(pc-2 , "0");
			
			int addressInt = Integer.parseInt(address,2); // the number of line we should jump to (decimal)
			if(opCode.equals("000010")){
				//JUMP
				for( ; addressInt - pc > 0 ; pc++){
					instructionMemory.add(nop);
				}
				pc  = addressInt ;
				rank = 0;
				
			}
			return  ;
		}
//---------------------------------------------------------------------------------------------------------	R-Execute
	public static String rExecute(R_Instruction ins){
		String func = ins.func ;
		int first = ins.first ;
		int sec = ins.second ;
		if(func.equals("100000")){
			//ADD
			//now we have the number of register we will get its value 
			int sum = sec + first ;
			String bsum = Integer.toBinaryString(sum) ;  // we convert the sum to binary String 
			int ln = bsum.length();
			for(int i= (32-ln) ; i>0 ; i--){
				bsum = 0 + bsum ;
			}
			return bsum ;
		}
		if(func.equals("100010")){
			//SUB
			int sub = first - sec ;
			String bsub = Integer.toBinaryString(sub) ;
			int ln = bsub.length();
			for(int i= (32-ln) ; i>0 ; i--){
				bsub = 0 + bsub ;
			}
			return bsub;
		}
		if(func.equals("100101")){
			//OR
			int or = sec | first ;
			String bor = Integer.toBinaryString(or) ;
			int ln = bor.length();
			for(int i= (32-ln) ; i>0 ; i--){
				bor = 0 + bor ;
			}
			return bor ;
		}
		if(func.equals("100100")){
			//AND
			int and = sec & first ;
			String band = Integer.toBinaryString(and) ;
			int ln = band.length();
			for(int i= (32-ln) ; i>0 ; i--){
				band = 0 + band ;
			}
			return band;
		}
		if(func.equals("100111")){
			//NOR
			int nor = ~ (sec | first) ;
			String bnor = Integer.toBinaryString(nor) ;
			int ln = bnor.length();
			for(int i= (32-ln) ; i>0 ; i--){
				bnor = 0 + bnor ;
			}
			return bnor;
		}
		if(func.equals("100110")){
			//XOR
			int xor = sec ^ first ;
			String bxor = Integer.toBinaryString(xor) ;
			int ln = bxor.length();
			for(int i= (32-ln) ; i>0 ; i--){
				bxor = 0 + bxor ;
			}
			return bxor;
		}
		if(func.equals("101010")){
			//SET LESS THAN 
			String one = Integer.toBinaryString(1) ;
			for(int i= 31 ; i>0 ; i--){
				one = 1 + one ;
			}
			if(first < sec){
				return one ;
			}
			return "00000000000000000000000000000000" ;
		}
		return null;
	}
//---------------------------------------------------------------------------------------------------------I-Execute
	public static int iExecute(I_Instruction ins){
		String opCode = ins.opCode ;
		int intRsValue = Integer.parseInt(ins.rsValue , 2) ;
		String rsValue = ins.rsValue ;
		int insRtValue = Integer.parseInt(ins.rtValue , 2) ;
		String rtValue = ins.rtValue ;
		int intOffset = ins.offset ;
		if(opCode.equals("100011")){
			//LW
			//rs is the one wich works with memory
			int memLoadIndex = intRsValue + intOffset ; // we will move on memory as much as offset + value of rs
			return memLoadIndex ;
		}
		if(opCode.equals("101011")){
			//SW
			//rs is the one wich works with memory
			int ln = rtValue.length(); 
			for(int i= (32-ln) ; i>0 ; i--){
				rtValue = 0 + rtValue ;
			}//make it 32bit
			int memSaveIndex = intRsValue + intOffset ; // we wlil move on memory as much as offset + value of rs
			ins.rtValue = rtValue ;
			return memSaveIndex;
		}
		if(opCode.equals("000100")){
			//BEQ
			
			int temp = intOffset ;
			if(rsValue.equals(rtValue)){
				for( ;  temp>1 ; temp-- ){
					instructionMemory.add(nop);
				}
				pc = (pc-3) + intOffset ;
				rank = 0 ;
				return 0 ;
			}
		}
		return 0 ;
	}
//-------------------------------------------------------------------------------------------------Memory
	public static void Mem(I_Instruction ins , int index ){
		if( pc-4 >= instructionMemory1.size()){
			return ;
		}
		if(ins.opCode.equals("100011")){
			//LW
			ins.mode = "LW" ;
			String toLoad = dataMemory.get(index);//keep the value which we got from memory in toLoad
			I_MEM_WB.add( pc-4 , toLoad) ;
		}
		if(ins.opCode.equals("101011")){
			//SW
			ins.mode = "SW" ;
			dataMemory.set(index, ins.rtValue);
		}
	}
//------------------------------------------------------------------------------------------------R-WB	
	public static void rWB(R_Instruction ins , String result){
		if( pc-5 >= instructionMemory1.size()){
			return ;
		}
		registerFile.set(ins.rdNo, result ) ;
	}
//------------------------------------------------------------------------------------------------I-WB
	public static void iWB(I_Instruction ins , String loaded){
		if( pc-5 >= instructionMemory1.size()){
			return ;
		}
		if(ins.mode.equals("SW")){
			return ;
		}
		registerFile.set(ins.rtNo , loaded ) ;
	}
//------------------------------------------------------------------------------------------------
	public static void createRegFile(){
		for(int n=0;n<32;n++){
			registerFile.add("00000000000000000000000000000000");
		}
		registerFile.set(18,"00000000000000000000000000011010") ; //s2
		registerFile.set(19,"00000000000000000000000000001110") ; //s3
		registerFile.set(20,"00000000000000000000000000001111") ; //s4
		registerFile.set(22,"00000000000000000000000000000010") ; //s6
		registerFile.set(23,"00000000000000000000000000001000") ; //s7
		registerFile.set(24,"00000000001111111100000000000000") ; //s8
	}
//------------------------------------------------------------------------------------------------
	public static void createMem(){
		//
		for(int n=0;n<256;n++){
			dataMemory.add("00000000000000000000000000000000");
		}
		dataMemory.set(24,"10101010101010101010101010101010");
		dataMemory.set(25,"00000000000000001111111111111111");
		dataMemory.set(19,"11111111111111110000000000000000");
	}
//------------------------------------------------------------------------------------------------
	public static void clock(){
		try{
			Thread.sleep(5000);
		}catch(Exception e){
			// 
		}
	}
//--------------------------------------------------------------------------------------------------
	public static void createMemFromFile() throws Exception {
		for(int n=0;n<128;n++){
		    String memValue = Files.readAllLines(Paths.get("src/DataMemory.txt")).get(n) ;
		    dataMemory.add(n , memValue);
		}
	}
//----------------------------------------------------------------------------------------------------
	public static void syncMem() throws Exception {
		for(int n=0;n<128;n++){
			List<String> lines = Files.readAllLines(Paths.get("src/DataMemory.txt")) ;
			lines.set(n , dataMemory.get(n));
			Files.write(Paths.get("src/DataMemory.txt"), lines); 
		}
	}
//---------------------------------------------------------------------------------------------------------------
	public static void exHazardDetection(){
		for(int i=0 ; i < instructionMemory1.size() ; i++){
			if(rds.get(i).equals(rss.get(i+1))){
				if(rds.get(i).equals("0")){
					//
				}else{
					System.out.println("EX HAZARD DETECTED (RS)!");
				}
			}
			if(rds.get(i).equals(rts.get(i+1))){
				if(rds.get(i).equals("0")){
					//
				}else{
					System.out.println("EX HAZARD DETECTED (RT)!");	
				}
			}
		}
	}
//------------------------------------------------------------------------------------------------------------------
	public static void memHazardDetection(){
		for(int i=0 ; i < instructionMemory1.size() ; i++){
			if(instructionMemory.get(i).type.equals("I")){
				if(rds.get(i).equals(rss.get(i+2))){
					if(rds.get(i).equals("0")){
						//
					}else{
						System.out.println("MEM HAZARD DETECTED (RS)!");
					}
				}
				if(rds.get(i).equals(rts.get(i+2))){
					if(rds.get(i).equals("0")){
						//
					}else{
						System.out.println("MEM HAZARD DETECTED (RT)!");	
					}
				}
			}
		}
	}
//----------------------------------------------------------------------------------------------------------------------	
}
