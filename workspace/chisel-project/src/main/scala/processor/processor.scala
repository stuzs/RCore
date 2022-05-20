package processor

import chisel3._
import chisel3.util._

//class processor(debug: Boolean) extends Module{
class processor extends Module{
  val io = IO(new Bundle{
    val ins_fetch_req = Output(Bool())
    val ins_pc = Output(UInt(32.W))
    val instruction = Input(UInt(32.W))
    val data_access_req = Output(Bool())
    val data_address = Output(UInt(32.W))
    val wen = Output(Bool())
    val wdata = Output(UInt(32.W))
    val rdata = Input(UInt(32.W))
    //just for test
    val stall = Output(UInt(32.W))
    val regFile = Output(Vec(32, UInt(32.W)))  //[0]-[31]  32bit unsigned each unit
  })//
  val level_1 = Module(new IF_level1).io // create component
  //val level_2 = Module(new DE_level2(debug)).io
  val level_2 = Module(new DE_level2).io
  val level_3 = Module(new EX_level3).io
  val level_4 = Module(new WB_level4).io
  //level_1 input
  level_1.instruction := io.instruction
  level_1.signFlush_l1 := level_4.signFlush_l1
  level_1.pcBran := level_3.pcBran
  level_1.signStall := level_2.signStall
  level_1.pcSel := level_3.pcSel

  io.ins_fetch_req := 1.B // always true Bool
  io.ins_pc := level_1.pc

  //level_2 input
  level_2.pc_l1 := level_1.pc_l1
  level_2.instruction_l1 := level_1.instruction_l1
  level_2.wAddress := level_4.wAddress   // can use <> to replace
  level_2.wEn := level_4.wEn
  level_2.wData := level_4.wData
  level_2.signFlush_l2 := level_4.signFlush_l2
  //level_3 input
  level_3.pc_l2 := level_2.pc_l2
  level_3.rsData1_l2:= level_2.rsData1_l2
  level_3.rsData2_l2 := level_2.rsData2_l2
  level_3.imme_l2 := level_2.imme_l2
  level_3.funct7_l2 := level_2.funct7_l2
  level_3.funct3_l2 := level_2.funct3_l2
  level_3.wAddress_l2 := level_2.wAddress_l2
  level_3.rsAddress1_l2 := level_2.rsAddress1_l2
  level_3.rsAddress2_l2 := level_2.rsAddress2_l2
  level_3.iType_l2 := level_2.iType_l2
  level_3.fwd_data1 := level_4.fwd_data1
  level_3.fwd_data2 := level_4.fwd_data2
  level_3.fwd_rs1 := level_4.fwd_rs1
  level_3.fwd_rs2 := level_4.fwd_rs2
  level_3.signFlush_l3 := level_4.signFlush_l3
  //io output
  io.data_access_req := level_3.data_access_req
  io.data_address := level_3.data_address
  io.wen := level_3.mem_wEn
  io.wdata := level_3.mem_wdata
  io.regFile := level_2.regFile
  //level_4 input
  level_4.signStall := level_2.signStall
  level_4.read1_nzero_l2 := level_2.read1_nzero_l2
  level_4.read2_nzero_l2 := level_2.read2_nzero_l2
  level_4.rsAddress1_l2 := level_2.rsAddress1_l2
  level_4.rsAddress2_l2 := level_2.rsAddress2_l2
  level_4.mem_to_reg_l3 := level_3.mem_to_reg_l3
  level_4.reg_write_l3 := level_3.reg_write_l3
  level_4.pcSel := level_3.pcSel
  level_4.result_l3 := level_3.result_l3
  level_4.wAddress_l3 := level_3.wAddress_l3
  level_4.rdata := io.rdata

  io.stall := 0.U
}


class IF_level1 extends Module{
    val io = IO(new Bundle{
        val pcSel = Input(Bool())
        val pcBran = Input(UInt(32.W))
        val pc = Output(UInt(32.W))
        val pc_l1 = Output(UInt(32.W))
        val instruction = Input(UInt(32.W))
        val instruction_l1 = Output(UInt(32.W))
        val signStall = Input(Bool())
        val signFlush_l1 = Input(Bool())
    })
    val pc_l1 = RegInit(0.U(32.W))
    val instruction_l1 = RegInit(0x13.U(32.W))
    val pc = RegInit(0.U(32.W))
    when(reset.asBool|io.signFlush_l1){
        pc_l1:=0x00000000.U
        instruction_l1:=0x00000013.U
    }.elsewhen(~io.signStall){
        pc_l1:=pc
        instruction_l1:=io.instruction
    }

    val pcNext = Mux(io.pcSel, io.pcBran, pc + 4.U)
    when(reset.asBool){
        pc:=0x00000000.U
    }.elsewhen(io.signStall){
        pc:=pc
    }.otherwise{
        pc:=pcNext
    }
    io.pc := pc
    io.pc_l1:=pc_l1
    io.instruction_l1:=instruction_l1
}


//class DE_level2(debug: Boolean) extends Module{
class DE_level2 extends Module{
    val io = IO(new Bundle{
        val pc_l1 = Input(UInt(32.W))
        val instruction_l1 = Input(UInt(32.W))
        val pc_l2 = Output(UInt(32.W))
        val signFlush_l2 = Input(Bool())

        val wData = Input(UInt(32.W))
        val wAddress = Input(UInt(5.W))  //5 bit for 32 registers
        val wEn = Input(Bool())

        val iType_l2 = Output(UInt(3.W))
        val rsData1_l2 = Output(UInt(32.W))
        val rsData2_l2 = Output(UInt(32.W))
        val rsAddress1_l2 = Output(UInt(5.W))
        val rsAddress2_l2 = Output(UInt(5.W))
        val wAddress_l2  = Output(UInt(5.W))
        val imme_l2 = Output(UInt(32.W))
        val funct7_l2 = Output(Bool())
        val funct3_l2 = Output(UInt(3.W))

        val read1_nzero_l2 = Output(Bool())
        val read2_nzero_l2 = Output(Bool())

        val signStall = Output(Bool())

        val regFile = Output(Vec(32, UInt(32.W)))
    })

    val iType = MuxCase("b111".U, Array(
       (io.instruction_l1(6, 2) === BitPat("b01100")) -> "b000".U,//OP add sub xor or and
       (io.instruction_l1(6, 2) === BitPat("b11001")) -> "b101".U,//JALR jalr
       (io.instruction_l1(6, 2) === BitPat("b00000")) -> "b011".U,//LOAD lw
       (io.instruction_l1(6, 2) === BitPat("b00100")) -> "b100".U,//OP_IMM addi xori ori andi
       (io.instruction_l1(6, 2) === BitPat("b01000")) -> "b010".U,//STORE sw
       (io.instruction_l1(6, 2) === BitPat("b11000")) -> "b001".U //BRANCH beq
    ))

    val rsAddress1 = io.instruction_l1(19,15) //rs1 is always here for implemented types
    val rsAddress2 = Mux(iType === "b100".U | iType === "b101".U | iType === "b011".U , 0.U , io.instruction_l1(24,20)) //some types have no rs2

    val register_file = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))
    when(reset.asBool){
        for(i <- 0 to 31){
            register_file(i) := 0.U
        }
    }.elsewhen(io.wEn && (io.wAddress =/= 0.U)) {  // wAddress==0 is x0
        register_file(io.wAddress) := io.wData
    }
    //32 register files set above

    //if(debug == true){
      for(i <- 0 until 32){
        io.regFile(i) := register_file(i)
      }
  //}

    val write_read1_gap = rsAddress1 === io.wAddress
    val write_read2_gap = rsAddress2 === io.wAddress
    val rsData1 = MuxCase(register_file(rsAddress1), Array(
        (rsAddress1 === 0.U) -> 0.U, // if rs1 indicates x0, its data is 0
        write_read1_gap -> io.wData  // foward pass gap type, i.e., rsData1 = newly to be written data
    ))
    val rsData2 = MuxCase(register_file(rsAddress2), Array(
        (rsAddress2 === 0.U) -> 0.U,
        write_read2_gap -> io.wData
    ))

    val imme = Wire(UInt(32.W))
    imme := MuxCase(Cat(Fill(21, io.instruction_l1(31)), io.instruction_l1(30, 20)), Array( //Cat for I type addi...
        (io.instruction_l1(6, 2) === "b01000".U) -> Cat(Fill(21, io.instruction_l1(31)), io.instruction_l1(30, 25), io.instruction_l1(11, 7)), //S type sw
        (io.instruction_l1(6, 2) === "b11000".U) -> Cat(Fill(20, io.instruction_l1(31)),io.instruction_l1(7), io.instruction_l1(30, 25), io.instruction_l1(11, 8), 0.U), //B type beq
        (io.instruction_l1(6, 2) === BitPat("b0?101")) -> Cat(io.instruction_l1(31), io.instruction_l1(30, 12), 0.U(12.W)), //U type lui auipc
        (io.instruction_l1(6, 2) === "b11011".U) -> Cat(Fill(12, io.instruction_l1(31)), io.instruction_l1(19, 12), io.instruction_l1(20), io.instruction_l1(30, 21), 0.U) //J type jal
    )) // imme number expansion

    val iType_l2 = Reg(UInt(3.W))
    val pc_l2 = Reg(UInt(32.W))
    val rsData1_l2 = Reg(UInt(32.W))
    val rsData2_l2 = Reg(UInt(32.W))
    val imme_l2 = Reg(UInt(32.W))
    val funct7_l2 = Reg(Bool())
    val funct3_l2 = Reg(UInt(3.W))
    val wAddress_l2 = Reg(UInt(5.W))
    val rsAddress1_l2 = Reg(UInt(5.W))
    val rsAddress2_l2 = Reg(UInt(5.W))
    val read1_nzero_l2 = Reg(Bool())
    val read2_nzero_l2 = Reg(Bool())

    when(reset.asBool | io.signFlush_l2){
        iType_l2 := "b111".U
        pc_l2 := 0.U
        rsData1_l2 := 0.U
        rsData2_l2 := 0.U
        imme_l2 := 0.U
        funct7_l2 := 0.B
        funct3_l2 := 0.U
        wAddress_l2 := 0.U
        rsAddress1_l2 := 0.U
        rsAddress2_l2 := 0.U
        read1_nzero_l2 := 0.B
        read2_nzero_l2 := 0.B
    }.otherwise{
        iType_l2 := iType
        pc_l2 := io.pc_l1
        rsData1_l2 := rsData1
        rsData2_l2 := rsData2
        imme_l2 := imme
        funct7_l2 := io.instruction_l1(30)
        funct3_l2 := io.instruction_l1(14,12)
        // if not Branch or Store type, wAddress_l2 is the rd to be written.
        wAddress_l2 := Mux(io.instruction_l1(6,2) === "b01000".U || io.instruction_l1(6,2) === "b11000".U, 0.U, io.instruction_l1(11,7))
        rsAddress1_l2 := rsAddress1
        rsAddress2_l2 := rsAddress2
        read1_nzero_l2 := rsAddress1 =/= 0.U
        read2_nzero_l2 := rsAddress2 =/= 0.U
    }

    io.iType_l2 := iType_l2
    io.pc_l2 := pc_l2
    io.rsData1_l2 := rsData1_l2
    io.rsData2_l2 := rsData2_l2
    io.imme_l2 := imme_l2
    io.rsAddress1_l2 := rsAddress1_l2
    io.rsAddress2_l2 := rsAddress2_l2
    io.read1_nzero_l2 := read1_nzero_l2
    io.read2_nzero_l2 := read2_nzero_l2
    io.funct7_l2 := funct7_l2
    io.funct3_l2 := funct3_l2
    io.wAddress_l2 := wAddress_l2
    val load_rs1 = wAddress_l2 === rsAddress1
    val load_rs2 = wAddress_l2 === rsAddress2
    val loadWhether = iType_l2 === "b011".U     // it is lw
    io.signStall := ((load_rs1 & (rsAddress1 =/= 0.U)) | (load_rs2 & (rsAddress2 =/= 0.U))) & loadWhether
}


class EX_level3 extends Module{
    val io = IO(new Bundle{
         val pc_l2 = Input(UInt(32.W))
         val rsData1_l2 = Input(UInt(32.W))
         val rsData2_l2 = Input(UInt(32.W))
         val imme_l2 = Input(UInt(32.W))
         val funct7_l2 = Input(UInt(32.W))
         val funct3_l2 = Input(UInt(32.W))
         val wAddress_l2 = Input(UInt(32.W))
         val rsAddress1_l2 = Input(UInt(32.W))
         val rsAddress2_l2 = Input(UInt(32.W))
         val iType_l2 = Input(UInt(3.W))
         val fwd_data1 = Input(UInt(32.W))
         val fwd_data2 = Input(UInt(32.W))
         val fwd_rs1 = Input(Bool())
         val fwd_rs2 = Input(Bool())
         val signFlush_l3 = Input(Bool())
         val data_access_req = Output(Bool())
         val data_address = Output(UInt(32.W))
         val mem_wEn = Output(Bool())
         val mem_wdata = Output(UInt(32.W))
         val pcBran = Output(UInt(32.W))
         val pcSel = Output(Bool()) 
         val reg_write_l3 = Output(Bool())
         val mem_to_reg_l3 = Output(Bool())
         val result_l3 = Output(UInt(32.W))
         val wAddress_l3 = Output(UInt(5.W))
    })
    val csignals = MuxCase("b000000000".U, Array(
      (io.iType_l2 === "b000".U) -> "b000000101".U,//OP add sub xor or and 
      (io.iType_l2 === "b101".U) -> "b010001100".U,//JALR jalr
      (io.iType_l2 === "b011".U) -> "b001101100".U,//LOAD lw
      (io.iType_l2 === "b100".U) -> "b000001110".U,//OP_IMM addi xori ori andi
      (io.iType_l2 === "b010".U) -> "b000011000".U,//STORE sw
      (io.iType_l2 === "b001".U) -> "b100000011".U //BRANCH beq
    ))
    // almost one bit for one action
    val branch = csignals(8)
    val jump = csignals(7)
    val mem_read = csignals(6)
    val mem_to_reg = csignals(5)
    val mem_write = csignals(4)
    val alu_src = csignals(3)
    val reg_write = csignals(2)
    val alu_ctrl = csignals(1,0) //last 2 bits of csignals

    val alu_op = Wire(UInt(5.W))
    when(alu_ctrl === "b01".U){
      when(io.funct3_l2 === "b000".U){ //SUB ADD
        alu_op := Mux(io.funct7_l2 =/= 0.U, "b01110".U, "b00110".U)
      }.elsewhen(io.funct3_l2 === "b100".U){
        alu_op := "b00010".U //XOR
      }.elsewhen(io.funct3_l2 === "b110".U){
        alu_op := "b00001".U //OR
      }.elsewhen(io.funct3_l2 === "b111".U){
        alu_op := "b00000".U //AND
      }.otherwise{alu_op := "b00110".U}
    }.elsewhen(alu_ctrl === "b10".U){
      when(io.funct3_l2 === "b000".U){
        alu_op := "b00110".U //ADDI
      }.elsewhen(io.funct3_l2 === "b100".U){
        alu_op := "b00010".U //XORI
      }.elsewhen(io.funct3_l2 === "b110".U){
        alu_op := "b00001".U //ORI
      }.elsewhen(io.funct3_l2 === "b111".U){
        alu_op := "b00000".U //AND
      }.otherwise{alu_op := "b00110".U}
    }.elsewhen(alu_ctrl === "b11".U){
      alu_op := "b01110".U //BEQ  sub to decide
    }.otherwise{
      alu_op := "b00110".U //ADD
    }

    val alu = Module(new ALU32b(32)).io // create 32-bit ALU, which has its opcode
    alu.alu_op := alu_op
    alu.a := Mux(io.fwd_rs1, io.fwd_data1, io.rsData1_l2)
    alu.b := MuxCase(io.rsData2_l2, Array(
                alu_src -> io.imme_l2,
                io.fwd_rs2 -> io.fwd_data2
              ))
    // some logic results can be easily got
    val slt_result = Mux(io.fwd_rs1, io.fwd_data1, io.rsData1_l2) < Mux(io.fwd_rs2, io.fwd_data2, io.rsData2_l2)
    val slti_result = Mux(io.fwd_rs1, io.fwd_data1, io.rsData1_l2) < io.imme_l2
    val blt_result = Mux(io.fwd_rs1, io.fwd_data1, io.rsData1_l2) < Mux(io.fwd_rs2, io.fwd_data2, io.rsData2_l2)
    val bne_result = Mux(io.fwd_rs1, io.fwd_data1, io.rsData1_l2) =/= Mux(io.fwd_rs2, io.fwd_data2, io.rsData2_l2)
    val bge_result = Mux(io.fwd_rs1, io.fwd_data1, io.rsData1_l2) >= Mux(io.fwd_rs2, io.fwd_data2, io.rsData2_l2)

    val mem_to_reg_l3 = Reg(Bool())
    val reg_write_l3 = Reg(Bool())
    val branch_beq_l3 = Reg(Bool())
    val branch_blt_l3 = Reg(Bool())
    val branch_bne_l3 = Reg(Bool())
    val branch_bge_l3 = Reg(Bool())
    val jump_l3 = Reg(Bool())
    val mem_read_l3 = Reg(Bool())
    val mem_write_l3 = Reg(Bool())
    val pc_l3 = Reg(UInt(32.W))
    val result_l3 = Reg(UInt(32.W))
    val zero_l3 = Reg(Bool())
    val rsData2_l3 = Reg(UInt(32.W))
    val wAddress_l3 = Reg(UInt(5.W))
    when(reset.asBool | io.signFlush_l3){
      mem_to_reg_l3 := 0.B
      reg_write_l3 := 0.B
      branch_beq_l3 := 0.B
      branch_blt_l3 := 0.B
      branch_bne_l3 := 0.B
      branch_bge_l3 := 0.B
      jump_l3 := 0.B
      mem_read_l3 := 0.B
      mem_write_l3 := 0.B
      pc_l3 := 0.B
      result_l3 := 0.B
      zero_l3 := 0.B
      rsData2_l3 := 0.B
      wAddress_l3 := 0.B
    }.otherwise{
      mem_to_reg_l3 := mem_to_reg
      reg_write_l3 := reg_write
      //branch_l3 := branch
      jump_l3 := jump
      mem_read_l3 := mem_read
      mem_write_l3 := mem_write
      pc_l3 := io.pc_l2 + io.imme_l2

      when(alu_ctrl === "b01".U && io.funct3_l2 === "b010".U){
        result_l3 := slt_result
      }.elsewhen(alu_ctrl === "b10".U && io.funct3_l2 === "b010".U){
         result_l3 := slti_result
      }.otherwise{
        result_l3 := alu.result
      }

      when(branch){
        when(io.funct3_l2 === "b000".U && alu.zero){
          branch_beq_l3 := 1.B
        }.elsewhen(io.funct3_l2 === "b001".U && bne_result){
          branch_bne_l3 := 1.B
        }.elsewhen(io.funct3_l2 === "b100".U && blt_result){
          branch_blt_l3 := 1.B
        }.elsewhen(io.funct3_l2 === "b101".U && bge_result){
          branch_bge_l3 := 1.B
        }
      }.otherwise{
         branch_beq_l3 := 0.B
         branch_blt_l3 := 0.B
         branch_bne_l3 := 0.B
         branch_bge_l3 := 0.B
      }
      //zero_l3 := alu.zero
      rsData2_l3 := Mux(io.fwd_rs2, io.fwd_data2, io.rsData2_l2)
      wAddress_l3 := io.wAddress_l2
    }
    io.data_access_req := mem_write_l3 | mem_read_l3
    io.data_address := result_l3
    io.mem_wEn := mem_write_l3
    io.mem_wdata := rsData2_l3
    io.pcBran := pc_l3
    io.pcSel := jump_l3 | branch_beq_l3 | branch_bne_l3 | branch_blt_l3 |branch_bge_l3
    io.reg_write_l3 := reg_write_l3
    io.mem_to_reg_l3 := mem_to_reg_l3
    io.wAddress_l3 := wAddress_l3
    io.result_l3 := result_l3
}


class WB_level4 extends Module{
    val io = IO(new Bundle{
        val signStall = Input(Bool())
        val read1_nzero_l2 = Input(Bool())
        val read2_nzero_l2 = Input(Bool())
        val rsAddress1_l2 = Input(UInt(5.W))
        val rsAddress2_l2 = Input(UInt(5.W))
        val mem_to_reg_l3 = Input(Bool())
        val reg_write_l3 = Input(Bool())
        val pcSel = Input(Bool())
        val result_l3 = Input(UInt(32.W))
        val wAddress_l3 = Input(UInt(5.W))
        val rdata = Input(UInt(32.W))
        val wData = Output(UInt(32.W))
        val wEn = Output(Bool())
        val fwd_rs1 = Output(Bool())
        val fwd_rs2 = Output(Bool())
        val fwd_data1 = Output(UInt(32.W))
        val fwd_data2 = Output(UInt(32.W))
        val signFlush_l1 = Output(Bool())
        val signFlush_l2 = Output(Bool())
        val signFlush_l3 = Output(Bool())
        val wAddress = Output(UInt(5.W))    
    })

    val mem_to_reg_l4 = Reg(Bool())
    val reg_write_l4 = Reg(Bool())
    when(reset.asBool){
      mem_to_reg_l4 := 0.B
      reg_write_l4 := 0.B
    }.otherwise{
      mem_to_reg_l4 := io.mem_to_reg_l3
      reg_write_l4 := io.reg_write_l3
    }
    val read_mem_data_l4 = RegNext(io.rdata)
    val result_l4 = RegNext(io.result_l3)
    val wAddress_l4 = RegNext(io.wAddress_l3)
    io.wAddress := wAddress_l4
    io.wData := Mux(mem_to_reg_l4, read_mem_data_l4, result_l4)
    io.wEn := reg_write_l4
    io.fwd_rs1 := io.reg_write_l3 & io.read1_nzero_l2 & 
                  (io.wAddress_l3 === io.rsAddress1_l2)|
                  reg_write_l4 & io.read1_nzero_l2 & 
                  (wAddress_l4 === io.rsAddress1_l2)
    io.fwd_rs2 := io.reg_write_l3 & io.read2_nzero_l2 & 
                   (io.wAddress_l3 === io.rsAddress2_l2)|
                  reg_write_l4 & io.read2_nzero_l2 & 
                  (wAddress_l4 === io.rsAddress2_l2)
    io.fwd_data1 := MuxCase(result_l4, Array(
                  (io.reg_write_l3 & io.read1_nzero_l2 & 
                   (io.wAddress_l3 === io.rsAddress1_l2)) -> io.result_l3,
                  (mem_to_reg_l4) -> read_mem_data_l4
                ))
    io.fwd_data2 := MuxCase(result_l4, Array(
                  (io.reg_write_l3 & io.read2_nzero_l2 & 
                  (io.wAddress_l3 === io.rsAddress2_l2)) -> io.result_l3,
                  mem_to_reg_l4 -> read_mem_data_l4
                ))
    io.signFlush_l1 := io.pcSel
    io.signFlush_l2 := io.signStall | io.pcSel
    io.signFlush_l3 := io.pcSel
}
