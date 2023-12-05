import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LEGv8Disassembler {
    public static void main(String[] args) {
        InsertInstructions();

        int instructionCount = 0;
        int[] instructions = null;
        try {
            RandomAccessFile file = new RandomAccessFile(args[0], "r");
            FileChannel channel = file.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate((int) channel.size()).order(ByteOrder.BIG_ENDIAN);
            channel.read(buffer);
            buffer.flip(); // Flip from little to big endian

            instructionCount = buffer.remaining() / 4;
            instructions = new int[instructionCount];
            for (int i = 0; i < instructionCount; i++) {
                instructions[i] = buffer.getInt();
            }
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<Instruction> instructionList = new ArrayList<>();

        for (int i = 0; i < instructionCount; i++) {
            int instruction = instructions[i];
            InstructionType instructionType = getInstructionType(instruction);
            if (instructionType == null) {
                System.out.println("Unknown instruction: " + Integer.toBinaryString(instruction));
                System.out.println(Integer.toHexString(instruction));
            } else {
                switch (instructionType) {
                    case R:
                        instructionList.add(new RInstruction(instruction));
                        break;
                    case I:
                        instructionList.add(new IInstruction(instruction));
                        break;
                    case D:
                        instructionList.add(new RInstruction(instruction));
                        break;
                    case B:
                        instructionList.add(new BInstruction(instruction));
                        break;        
                    case CB:
                        instructionList.add(new CBInstruction(instruction));
                        break;
                }
            }
        }

        for (Instruction instruction : instructionList) {
            System.out.println(instruction.toString());
        }
    }

    enum InstructionType {
        R, I, D, CB, B
    }

    private static InstructionType getInstructionType(int instruction) {
        int opcode = (instruction >>> 21); // R, and D instructions
        if (instructionMap.containsKey(opcode)) {
            return InstructionType.R;
        }
        opcode = (opcode >>> 1); // I instructions
        if (instructionMap.containsKey(opcode)) {
            return InstructionType.I;
        }
        opcode = (opcode >>> 2); // CB instructions
        if (instructionMap.containsKey(opcode)) {
            return InstructionType.CB;
        }
        opcode = (opcode >>> 2); // B instructions
        if (instructionMap.containsKey(opcode)) {
            return InstructionType.B;
        }
        return null;
    }

    static final Map<Integer, String> instructionMap = new HashMap<>();

    public static void InsertInstructions() {
        instructionMap.put(0b10001011000, "ADD");
        instructionMap.put(0b1001000100, "ADDI");
        instructionMap.put(0b1011000100, "ADDIS");
        instructionMap.put(0b10101011000, "ADDS");
        instructionMap.put(0b10001010000, "AND");
        instructionMap.put(0b1001001000, "ANDI");
        instructionMap.put(0b1111001000, "ANDIS");
        instructionMap.put(0b1110101000, "ANDS");
        instructionMap.put(0b000101, "B");
        instructionMap.put(0b100101, "BL");
        instructionMap.put(0b11010110000, "BR");
        instructionMap.put(0b10110101, "CBNZ");
        instructionMap.put(0b10110100, "CBZ");
        instructionMap.put(0b11111111110, "DUMP");
        instructionMap.put(0b11001010000, "EQR");
        instructionMap.put(0b1101001000, "EQRI");
        instructionMap.put(0b00011110011, "FADDD");
        instructionMap.put(0b00011110001, "FADDS");
        instructionMap.put(0b00011110011, "FCMPD");
        instructionMap.put(0b00011110001, "FCMPS");
        instructionMap.put(0b00011110011, "FDIVD");
        instructionMap.put(0b00011110001, "FDIVS");
        instructionMap.put(0b00011110011, "FMULD");
        instructionMap.put(0b00011110001, "FMULS");
        instructionMap.put(0b00011110011, "FSUBD");
        instructionMap.put(0b00011110001, "FSUBS");
        instructionMap.put(0b11111111111, "HALT");
        instructionMap.put(0b11111000010, "LDUR");
        instructionMap.put(0b00111000010, "LDURB");
        instructionMap.put(0b11111100010, "LDURD");
        instructionMap.put(0b01111000010, "LDURH");
        instructionMap.put(0b10111100010, "LDURS");
        instructionMap.put(0b10111000100, "LDURSW");
        instructionMap.put(0b11010011011, "LSL");
        instructionMap.put(0b11010011010, "LSR");
        instructionMap.put(0b10011011000, "MUL");
        instructionMap.put(0b10101010000, "ORR");
        instructionMap.put(0b1011001000, "ORRI");
        instructionMap.put(0b11111111100, "PRNL");
        instructionMap.put(0b11111111101, "PRNT");
        instructionMap.put(0b10011010110, "SDIV");
        instructionMap.put(0b10011011010, "SMULH");
        instructionMap.put(0b11111000000, "STUR");
        instructionMap.put(0b00111000000, "STURB");
        instructionMap.put(0b11111100000, "STURD");
        instructionMap.put(0b01111000000, "STURH");
        instructionMap.put(0b10111100000, "STURS");
        instructionMap.put(0b10111000000, "STURSW");
        instructionMap.put(0b11001011000, "SUB");
        instructionMap.put(0b1101000100, "SUBI");
        instructionMap.put(0b1111000100, "SUBIS");
        instructionMap.put(0b11101011000, "SUBS");
        instructionMap.put(0b10011010110, "UDIV");
        instructionMap.put(0b10011011110, "UMULH");
    }
}

abstract class Instruction {
    public abstract String toString();
}

class RInstruction extends Instruction {
    int opcode;
    int Rm;
    int shamt;
    int Rn;
    int Rd;
    String name;

    RInstruction(int instruction) {
        opcode = (instruction >>> 21); // Extract bits 21-31
        Rm = (instruction >>> 16) & 0x1F; // Extract bits 16-20
        shamt = (instruction >>> 10) & 0x3F; // Extract bits 10-15
        Rn = (instruction >>> 5) & 0x1F; // Extract bits 5-9
        Rd = instruction & 0x1F; // Extract bits 0-4
        name = LEGv8Disassembler.instructionMap.get(opcode);
    }

    @Override
    public String toString() {
        return name + " X" + Rd + ", X" + Rn + ", X" + Rm;
    }
}

class IInstruction extends Instruction {
    int opcode;
    int ALU_immediate;
    int Rn;
    int Rd;
    String name;

    IInstruction(int instruction) {
        opcode = (instruction >>> 22); // Extract bits 21-31
        ALU_immediate = (instruction >>> 10) & 0xFFF; // Extract bits 10-21
        Rn = (instruction >>> 5) & 0x1F; // Extract bits 5-9
        Rd = instruction & 0x1F; // Extract bits 0-4
        name = LEGv8Disassembler.instructionMap.get(opcode);
    }

    @Override
    public String toString() {
        return name + " X" + Rd + ", X" + Rn + ", #" + ALU_immediate;
    }
}

class DInstruction extends Instruction {
    int opcode;
    int DT_address;
    int op;
    int Rn;
    int Rt;
    String name;

    DInstruction(int instruction) {
        opcode = (instruction >>> 21); // Extract bits 21-31
        DT_address = (instruction >>> 12) & 0x1FF; // Extract bits 12-20
        op = (instruction >>> 10) & 0x3; // Extract bits 10-11
        Rn = (instruction >>> 5) & 0x1F; // Extract bits 5-9
        Rt = instruction & 0x1F; // Extract bits 0-4
        name = LEGv8Disassembler.instructionMap.get(opcode);
    }

    @Override
    public String toString() {
        return name + " X" + Rt + ", [X" + Rn + ", #" + DT_address + "]";
    }
}

class BInstruction extends Instruction {
    int opcode;
    int BR_address;
    String name;

    BInstruction(int instruction) {
        opcode = (instruction >>> 26); // Extract bits 26-31
        BR_address = instruction & 0x3FFFFFF; // Extract bits 0-25
        name = LEGv8Disassembler.instructionMap.get(opcode);
    }

    @Override
    public String toString() {
        return name + " #" + BR_address;
    }
}

class CBInstruction extends Instruction {
    int opcode;
    int COND_BR_address;
    int Rt;
    String name;

    CBInstruction(int instruction) {
        opcode = (instruction >>> 24); // Extract bits 24-31
        COND_BR_address = (instruction >>> 5) & 0x7FFFF; // Extract bits 5-23
        Rt = instruction & 0x1F; // Extract bits 0-4
        name = LEGv8Disassembler.instructionMap.get(opcode);
    }

    @Override
    public String toString() {
        return name + " X" + Rt + ", #" + COND_BR_address;
    }
}