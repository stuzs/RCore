package processor

import chisel3._

/*
  How to run:
  sbt "test:runMain processor.genProcessor --target-dir generated/processor"
 */

object genALU1b extends App {
  Driver.execute(args, () => new ALU1b)
}

object genALU32b extends App {
  Driver.execute(args, () => new ALU32b(args(0).toInt))
}

object genProcessor extends App{
  //Driver.execute(args, () => new processor(debug = false))
  Driver.execute(args, () => new processor)
}

object genTopModule extends App{
  val inst = config.path.concat("/workspace/chisel-project/src/test/").concat(config.pattern)
  val data = config.path.concat("/workspace/chisel-project/src/test/data_init.pat")
  Driver.execute(args, () => new testTopModule(inst, data))
}

object genL1 extends App{
  Driver.execute(args, () => new IF_level1)
}

object genL2 extends App{
  //Driver.execute(args, () => new DE_level2(debug = false))
  Driver.execute(args, () => new DE_level2)
}

object genL3 extends App{
  Driver.execute(args, () => new EX_level3)
}

object genL4 extends App{
  Driver.execute(args, () => new WB_level4)
}

 