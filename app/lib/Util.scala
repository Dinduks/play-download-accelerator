package lib

import java.nio.ByteBuffer
import java.nio.channels.FileChannel

object Util {
  def allocateFile(fileChannel: FileChannel, size: Int) = fileChannel.write(ByteBuffer.allocate(size))
}
