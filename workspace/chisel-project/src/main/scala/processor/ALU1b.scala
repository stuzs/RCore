package processor

import chisel3._

class ALU1b extends Module {
  val io = IO(new Bundle {
    //control signal
    val inv_a = Input(Bool())
    val inv_b = Input(Bool())
    val c_valid = Input(Bool())
    val choose = Input(UInt(2.W))
    //input signal
    val a = Input(Bool())
    val b = Input(Bool())
    //val less = Input(Bool())
    val carry_in = Input(Bool())
    //output signal
    val result = Output(Bool())
    val carry_out = Output(Bool())
  })
  val a_in = Mux(io.inv_a, ~io.a, io.a)
  val b_in = Mux(io.inv_b, ~io.b, io.b)
  val c_in = io.c_valid & io.carry_in
  val and_result = a_in & b_in
  val or_result = a_in | b_in
  val sum = a_in ^ b_in ^ c_in
  io.carry_out := a_in & b_in | a_in & c_in | b_in & c_in
  io.result := Mux(io.choose(1),
               Mux(io.choose(0), 0.U/*io.less*/, sum),
               Mux(io.choose(0), or_result, and_result)
              )
}
