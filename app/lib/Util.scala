package lib

import java.nio.ByteBuffer
import java.nio.channels.FileChannel

object Util {

  def allocateFile(fileChannel: FileChannel, size: Int) = fileChannel.write(ByteBuffer.allocate(size))

  def getEndOffset(responseLength: Int, connection: Int, connectionsNumber: Int = 4): Int =
    if (connection == connectionsNumber) responseLength - 1 else ((responseLength + (4 - responseLength % 4)) / 4) * connection - 1;

  def getStartOffset(responseLength: Int, connection: Int, connectionsNumber: Int = 4): Int =
    ((responseLength + (4 - responseLength % 4)) / 4) * (connection - 1);

  def getStartAndEndOffsets(responseLength: Int, connection: Int, connectionsNumber: Int = 4) = {
    (getStartOffset(responseLength, connection, connectionsNumber), getEndOffset(responseLength, connection, connectionsNumber))
  }
}
