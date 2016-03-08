package native
package compiler
package pass

import scala.collection.mutable
import scala.util.control.Breaks._
import native.nir._
import native.util.unsupported

/** Eliminates:
 *  - Type.Nothing
 */
class NothingLowering extends Pass {
  override def preBlock = { case Block(n, params, insts, cf) =>
    val ninsts = mutable.UnrolledBuffer.empty[Inst]
    var ncf = cf
    breakable {
      insts.foreach {
        case Inst(_, call: Op.Call) if call.resty == Type.Nothing =>
          ninsts += Inst(call)
          ncf     = Cf.Unreachable
          break
        case inst if inst.op.resty == Type.Nothing =>
          unsupported("only calls can return nothing")
        case inst =>
          ninsts += inst
      }
    }
    Seq(Block(n, params, ninsts.toSeq, ncf))
  }

  override def preType = {
    case Type.Nothing =>
      unsupported("nothing can only be used as the result type of the function")
    case Type.Function(params, Type.Nothing) =>
      Type.Function(params, Type.Void)
  }
}