package processor

import chisel3._
import chisel3.util.experimental.loadMemoryFromFile

class MemoryIO extends Bundle{
  val sel = Input(Bool())
  val addr = Input(UInt(32.W))
  val wen = Input(Bool())
  val wdata = Input(UInt(32.W))
  val rdata = Output(UInt(32.W))
}

class memory(inst_file: String, data_init: String) extends Module{
  val io = IO(new Bundle{
    val ins_IO = new MemoryIO
    val data_IO = new MemoryIO
    val dataMem = Output(Vec(32, UInt(32.W)))
  })
  val ins_data_cell = Mem(32, UInt(32.W))
  when((io.ins_IO.sel && io.ins_IO.wen) === 1.B){
    ins_data_cell.write(io.ins_IO.addr(6, 2), io.ins_IO.wdata)
  }
  io.ins_IO.rdata := ins_data_cell.read(io.ins_IO.addr(6, 2))

  val data_data_cell = Mem(32, UInt(32.W))
  when((io.data_IO.sel && io.data_IO.wen) === 1.B){
    data_data_cell.write(io.data_IO.addr(6, 2), io.data_IO.wdata)
  }
  io.data_IO.rdata := data_data_cell.read(io.data_IO.addr(6, 2))
  loadMemoryFromFile(ins_data_cell, inst_file)
  loadMemoryFromFile(data_data_cell, data_init)

  for(i <- 0 until 32){
    io.dataMem(i) := data_data_cell.read(i.asUInt)
  }
}

class testTopModule(inst_file: String, data_init: String) extends Module{
 // val debug = true
  val io = IO(new Bundle{
    val valid = Input(Bool())
    val ins = Output(UInt(32.W))
    val stall = Output(UInt(32.W))
    val regFile = Output(Vec(32, UInt(32.W)))
    val dataMem = Output(Vec(32, UInt(32.W)))
  })
  val memory = Module(new memory(inst_file = inst_file,
                                 data_init = data_init))
  //val unit = Module(new processor(debug))
  val unit = Module(new processor)

  memory.io.ins_IO.sel := unit.io.ins_fetch_req
  memory.io.ins_IO.addr := unit.io.ins_pc
  memory.io.ins_IO.wen := 0.B
  memory.io.ins_IO.wdata := 0.U
  unit.io.instruction := memory.io.ins_IO.rdata

  memory.io.data_IO.sel := unit.io.data_access_req
  memory.io.data_IO.addr := unit.io.data_address
  memory.io.data_IO.wen := unit.io.wen
  memory.io.data_IO.wdata := unit.io.wdata
  unit.io.rdata := memory.io.data_IO.rdata

  io.ins := unit.io.ins_pc
  io.stall := unit.io.stall
  io.regFile := unit.io.regFile
  io.dataMem := memory.io.dataMem
}
