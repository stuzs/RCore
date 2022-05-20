package processor

import chisel3.iotesters._
import scala.util._
import scala._
import scala.io._
import chisel3._
import chisel3.util._
import java.io.PrintWriter
import java.io.File
import sys.process._
import scala.util.matching.Regex

object config{
  val path = sys.env("CHISEL_WORKSPACE_PATH")
  val pattern = "gcd.pat"
}

/*
  How to run:
  sbt "test:runMain processor.genTestALU32b --target-dir generated/ALU32btest --is-verbose"
  sbt "test:runMain processor.genTestProcessor --target-dir generated/Processortest --is-verbose"
  sbt "test:runMain processor.genRandomTestProcessor --target-dir generated/Processortest --is-verbose"
  Generate vcd waveform file:
  sbt "test:runMain processor.genTestProcessor --target-dir generated/Processortest --backend-name verilator"
 */
class testALU32b(unit: ALU32b) extends PeekPokeTester(unit){
  val randNum = new Random
  for(i <- 0 until 10){
    val a = randNum.nextInt(1 << 30).toLong
    val b = randNum.nextInt(1 << 30).toLong
    poke(unit.io.alu_op, 6)//ADD
    poke(unit.io.a, a)
    poke(unit.io.b, b)
    step(1)
    expect(unit.io.result, (a + b) & 0xffffffffL)
    poke(unit.io.alu_op, 14)//SUB
    poke(unit.io.a, a)
    poke(unit.io.b, b)
    step(1)
    expect(unit.io.result, (a - b) & 0xffffffffL)
    poke(unit.io.alu_op, 2)//XOR
    poke(unit.io.a, a)
    poke(unit.io.b, b)
    step(1)
    expect(unit.io.result, (a ^ b) & 0xffffffffL)
    poke(unit.io.alu_op, 1)//OR
    poke(unit.io.a, a)
    poke(unit.io.b, b)
    step(1)
    expect(unit.io.result, (a | b) & 0xffffffffL)
    poke(unit.io.alu_op, 0)//AND
    poke(unit.io.a, a)
    poke(unit.io.b, b)
    step(1)
    expect(unit.io.result, (a & b) & 0xffffffffL)
  }
}

object genTestALU32b extends App{
  chisel3.iotesters.Driver.execute(args, () => new ALU32b(32))(unit => new testALU32b(unit))
}

class testProcessor(unit: testTopModule) extends PeekPokeTester(unit){
  reset(1)
  poke(unit.io.valid, 1)
  while(peek(unit.io.ins) < 128){
    step(1)
  }
}

object genTestProcessor extends App{
  /* define the files for instruction and memory data pattern file */
  val inst = config.path.concat("/workspace/chisel-project/src/test/").concat(config.pattern)
  val data = config.path.concat("/workspace/chisel-project/src/test/data_init.pat")
  chisel3.iotesters.Driver.execute(args, () => new testTopModule(inst, data))(unit => new testProcessor(unit))
}

class randomTestProcessor(unit: testTopModule) extends PeekPokeTester(unit){
  def getAnswer(address: String): (Array[Int], Array[Int]) = {
    val code = Source.fromFile(address)
    //val element = inst.split("  | |, |\\(|\\)").toList.filter(s => s.length > 0)
    val dataMem, regFile = new Array[Int](32)
    val data = Source.fromFile(config.path.concat("/workspace/chisel-project/src/test/data_init.pat")).getLines.toList
    for(i <- 0 until 32){
      //implicit def hex2int(hex: String): Int = Integer.parseInt(hex, 16)
      dataMem(i) = data(i).toList.map("0123456789abcdef".indexOf(_)).reduceLeft(_ * 16 + _)
    }
    val instMem = code.getLines.toArray
    var pc, round = 0
    def ALU(inst_current: String): Int = {
      //not real inst format. It's like: xxx rd, rs1[offset], rs2[imm]
      val inst = inst_current.split("  | |, |\\(|\\)").toList.filter(s => s.length > 0)
      val rd = if(("x".r findFirstIn inst(1)) != None) inst(1).stripPrefix("x").toInt else 0
      val rs1 = if(("x".r findFirstIn inst(2)) != None) inst(2).stripPrefix("x").toInt else 0
      val rs2 = if(("x".r findFirstIn inst(3)) != None) inst(3).stripPrefix("x").toInt else 0
      val imm = if(("^(?!.*x).*$".r findFirstIn inst(3)) != None) inst(3).toInt else 0
      val offset = if(("^(?!.*x).*$".r findFirstIn inst(2)) != None) inst(2).toInt else 0     
      var next_pc = pc + 1
      inst(0) match{
        case "add" => {
          regFile(rd) = regFile(rs1) + regFile(rs2)
        }
        case "sub" => {
          regFile(rd) = regFile(rs1) - regFile(rs2)
        }
        case "xor" => {
          regFile(rd) = regFile(rs1) ^ regFile(rs2)
        }
        case "or" => {
          regFile(rd) = regFile(rs1) | regFile(rs2)
        }
        case "and" => {
          regFile(rd) = regFile(rs1) & regFile(rs2)
        }
        case "beq" => {
          if(regFile(rd) == regFile(rs1)) next_pc = pc + imm / 4
        }
        case "lw" => {
          val index = (regFile(rs2) + offset / 4) & 0x1fL //%32
          regFile(rd) = dataMem(index)
        }
        case "sw" => {
          val index = (regFile(rs2) + offset / 4) & 0x1fL
          dataMem(index) = regFile(rd)
        }
        case "addi" => {
          regFile(rd) = regFile(rs1) + imm
        }
        case "xori" => {
          regFile(rd) = regFile(rs1) ^ imm
        }
        case "ori" => {
          regFile(rd) = regFile(rs1) | imm
        }
        case "andi" => {
          regFile(rd) = regFile(rs1) & imm
        }
        case _ => {

        }
      }
      regFile(0) = 0
      return next_pc
    }
    while(pc < 32){
      pc = ALU(instMem(pc)) 
      round += 1
    }
    return (dataMem, regFile)
  }
  val Answer = getAnswer(config.path.concat("/workspace/chisel-project/src/test/assembly/RandomProgram.s"))
  reset(1)
  poke(unit.io.valid, 1)
  while(peek(unit.io.ins) < 128){
    step(1)
  }
  for(i <- 0 until 32){
    expect(unit.io.regFile(i), Answer._2(i) & 0xffffffffL)
  }
  for(i <- 0 until 32){
    expect(unit.io.dataMem(i), Answer._1(i) & 0xffffffffL)
  }
}

object genRandomTestProcessor extends App{
  def randomInstGen(length: Int, address: String): Unit = {
    val randNum = new Random
    val writer = new PrintWriter(new File(address))
    object inst extends Enumeration{
      type inst = Value
      val add, sub, xor, or, and, beq, sw, lw, addi, xori, ori, andi, nop = Value
    }
    val R_type = (rd: Int, rs1: Int, rs2: Int) => writer.write(s"x$rd, x$rs1, x$rs2\n")
    val B_type = (rs1: Int, rs2: Int, offset: Int) => writer.write(s"x$rs1, x$rs2, $offset\n")
    val S_type = (rs2: Int, offset: Int, rs1: Int) => writer.write(s"x$rs2, $offset(x$rs1)\n")
    val I_type = (rd: Int, rs1: Int, imm: Int) => writer.write(s"x$rd, x$rs1, $imm\n")
    for(i <- 0 until length){
      val inst_type = if(i < length - 5) randNum.nextInt(12) else 12
      val rd, rs1, rs2 = randNum.nextInt(32)
      val imm = randNum.nextInt(10)
      val limit = Array(6, i, 26 - i).map(n => n).min
      val offset = if(i < 27) 4 * randNum.nextInt(limit + 1) else 0
      val sign_imm, sign_offset = randNum.nextInt(2)
      inst(inst_type) match{
        case inst.add => {
          writer.write("  add ")
          R_type(rd, rs1, rs2)
        }
        case inst.sub => {
          writer.write("  sub ")
          R_type(rd, rs1, rs2)
        }
        case inst.xor => {
          writer.write("  xor ")
          R_type(rd, rs1, rs2)
        }
        case inst.or => {
          writer.write("  or ")
          R_type(rd, rs1, rs2)
        }
        case inst.and => {
          writer.write("  and ")
          R_type(rd, rs1, rs2)
        }
        case inst.beq => {
          writer.write("  beq ")
          B_type(rs1, rs2, if(offset == 0) 4 else offset)
        }
        case inst.sw => {
          writer.write("  sw ")
          S_type(rs2, if(sign_offset == 1) offset else -offset, 0)
        }
        case inst.lw => {
          writer.write("  lw ")
          S_type(rs2, if(sign_offset == 1) offset else -offset, 0)
        }
        case inst.addi => {
          writer.write("  addi ")
          I_type(rd, rs1, if(sign_imm == 1) imm else -imm)
        }
        case inst.xori => {
          writer.write("  xori ")
          I_type(rd, rs1, if(sign_imm == 1) imm else -imm)
        }
        case inst.ori => {
          writer.write("  ori ")
          I_type(rd, rs1, if(sign_imm == 1) imm else -imm)
        }
        case inst.andi => {
          writer.write("  andi ")
          I_type(rd, rs1, if(sign_imm == 1) imm else -imm)
        }
        case _ => {writer.write("  addi x0, x0, 0\n")}//nop
      }
    }
    writer.close
    //("riscv32-unknown-elf-as ./src/test/assembly/RandomProgram.s -o ./src/test/assembly/RandomProgram").!
  }
  val assembly = config.path.concat("/workspace/chisel-project/src/test/assembly/RandomProgram.s")
  randomInstGen(32, assembly)
  /*
    val test = (line: Array[String]) => {
      line.foreach((s: String) => tester.write(s"$s "))
    } 
    test(element)
    tester.write("\n")
   */
  def liteAssembler(input: String, output: String): Unit = {
    val code = Source.fromFile(input)
    val opcode = Map("add" -> "0110011",
                  "sub" -> "0110011",
                  "xor" -> "0110011",
                  "or" -> "0110011",
                  "and" -> "0110011",
                  "lw" -> "0000011",
                  "addi" -> "0010011",
                  "xori" -> "0010011",
                  "ori" -> "0010011",
                  "andi" -> "0010011",
                  "sw" -> "0100011",
                  "beq" -> "1100011")
    val funct7 = Map("add" -> "0000000",
                     "sub" -> "0100000",
                     "xor" -> "0000000",
                     "or" -> "0000000",
                     "and" -> "0000000")
    val funct3 = Map("add" -> "000",
                     "sub" -> "000",
                     "xor" -> "100",
                     "or" -> "110",
                     "and" -> "111",
                     "lw" -> "010",
                     "sw" -> "010",
                     "beq" -> "000",
                     "addi" -> "000",
                     "xori" -> "100",
                     "ori" -> "110",
                     "andi" -> "111")
    def stringToHex(s: String): String = {
      val binaryToHex = Map("0000" -> "0",
                            "0001" -> "1",
                            "0010" -> "2",
                            "0011" -> "3",
                            "0100" -> "4",
                            "0101" -> "5",
                            "0110" -> "6",
                            "0111" -> "7",
                            "1000" -> "8",
                            "1001" -> "9",
                            "1010" -> "a",
                            "1011" -> "b",
                            "1100" -> "c",
                            "1101" -> "d",
                            "1110" -> "e",
                            "1111" -> "f")
      def transform(s: String, round: Int): String = {
        return if(round > 0) binaryToHex(s.substring(32 - round * 4, 36 - round * 4)).concat(transform(s, round - 1)) else ""
      }
      return transform(s, 8)
    }
    def intToBinaryString(n: Int, bitWidth: Int): String = {
      def transform(n: Int, bit: Int): String = {
         return if(bit - 1 > 0) (((1 << (bit - 1)) & n) >>> (bit - 1)).toString.concat(transform(n, bit - 1)) else (0x00000001 & n).toString
      }
      return transform(n, 32).substring(32 - bitWidth, 32)
    }
    /*
    print(intToBinaryString(-24, 12))
    print("\n")
    */
    val writer = new PrintWriter(new File(output))
    for(inst <- code.getLines){
      val element = inst.split("  | |, |\\(|\\)").toList.filter(s => s.length > 0)
      /*
      val test = (line: List[String]) => {
        line.foreach((s: String) => {tester.write(s"$s "); print(s"!$s")})
        print("\n")
      }
      test(element)
      tester.write("\n")
      */
      val rd = if(("x".r findFirstIn element(1)) != None) element(1).stripPrefix("x").toInt else 0
      val rs1 = if(("x".r findFirstIn element(2)) != None) element(2).stripPrefix("x").toInt else 0
      val rs2 = if(("x".r findFirstIn element(3)) != None) element(3).stripPrefix("x").toInt else 0
      val imm = if(("^(?!.*x).*$".r findFirstIn element(3)) != None) element(3).toInt else 0
      val offset = if(("^(?!.*x).*$".r findFirstIn element(2)) != None) element(2).toInt else 0
      val R_type = (inst: String) => writer.write(stringToHex(funct7(inst).concat(intToBinaryString(rs2, 5)).concat(intToBinaryString(rs1, 5))
                      .concat(funct3(inst)).concat(intToBinaryString(rd, 5)).concat(opcode(inst))).concat("\n"))
      val B_type = (inst: String) => writer.write(stringToHex(intToBinaryString(imm, 13).substring(0, 1)
                      .concat(intToBinaryString(imm, 13).substring(2, 8))
                      .concat(intToBinaryString(rs1, 5)).concat(intToBinaryString(rd, 5)).concat(funct3(inst))
                      .concat(intToBinaryString(imm, 13).substring(8, 12))
                      .concat(intToBinaryString(imm, 13).substring(1, 2))
                      .concat(opcode(inst))).concat("\n"))
      val I_type_lw = (inst: String) => writer.write(stringToHex(intToBinaryString(offset, 12)
                      .concat(intToBinaryString(rs2, 5))
                      .concat(funct3(inst)).concat(intToBinaryString(rd, 5))
                      .concat(opcode(inst))).concat("\n"))
      val I_type_sw = (inst: String) => writer.write(stringToHex(intToBinaryString(offset, 12).substring(0, 7)
                      .concat(intToBinaryString(rd, 5)).concat(intToBinaryString(rs2, 5)).concat(funct3(inst))
                      .concat(intToBinaryString(offset, 12).substring(7, 12))
                      .concat(opcode(inst))).concat("\n"))
      val I_type = (inst: String) => writer.write(stringToHex(intToBinaryString(imm, 12)
                      .concat(intToBinaryString(rs1, 5)).concat(funct3(inst))
                      .concat(intToBinaryString(rd, 5)).concat(opcode(inst))).concat("\n"))
      val nop_type = (inst: String) => writer.write("00000013\n")//addi x0, x0, 0
      element(0) match{
        case "add" => {
          R_type("add")
        }
        case "sub" => {
          R_type("sub")
        }
        case "xor" => {
          R_type("xor")
        }
        case "or" => {
          R_type("or")
        }
        case "and" => {
          R_type("and")
        }
        case "beq" => {
          B_type("beq")
        }
        case "lw" => {
          I_type_lw("lw")
        }
        case "sw" => {
          I_type_sw("sw")
        }
        case "addi" => {
          I_type("addi")
        }
        case "xori" => {
          I_type("xori")
        }
        case "ori" => {
          I_type("ori")
        }
        case "andi" => {
          I_type("andi")
        }
        case _ => {
          nop_type("nop")
        }
      }
    }
    code.close
    writer.close
  }
  val inst = config.path.concat("/workspace/chisel-project/src/test/assembly/RandomProgram.txt")
  liteAssembler(assembly, inst)
  val data = config.path.concat("/workspace/chisel-project/src/test/data_init.pat")
  chisel3.iotesters.Driver.execute(args, () => new testTopModule(inst, data))(unit => new randomTestProcessor(unit))
}
