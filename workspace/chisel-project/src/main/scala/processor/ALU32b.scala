package processor

import chisel3._

class ALU32b(n: Int) extends Module{
  val io = IO(new Bundle{
    val alu_op = Input(UInt(5.W))
    val a = Input(UInt(n.W))
    val b = Input(UInt(n.W))
    val result = Output(UInt(n.W))
    val overflow = Output(Bool())
    val zero = Output(Bool())
  })
  val r = VecInit(io.a.asBools)
  val alu_array = VecInit(Seq.fill(n + 1)(Module(new ALU1b).io))
  alu_array(0).carry_in := io.alu_op(3)
  alu_array(n).a := io.a(n - 1)
  alu_array(n).b := io.b(n - 1)
  for(m <- 0 to n){
    alu_array(m).inv_a := io.alu_op(4)
    alu_array(m).inv_b := io.alu_op(3)
    alu_array(m).c_valid := io.alu_op(2)
    alu_array(m).choose := io.alu_op(1, 0)
  }
  for(m <- 0 until n){
    r(m) := alu_array(m).result
    alu_array(m).a := io.a(m)
    alu_array(m).b := io.b(m)
  }
  for(m <- 1 to n){
    alu_array(m).carry_in := alu_array(m - 1).carry_out
  }
  io.result := r.asUInt
  io.zero := io.result === 0.U
  io.overflow := alu_array(n).result ^ io.result(n - 1)
}