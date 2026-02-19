package example;

// Subsystem 1
class CPU {
    public void freeze() {
        System.out.println("CPU Freeze");
    }

    public void jump(long position) {
        System.out.println("CPU Jump");
    }

    public void execute() {
        System.out.println("CPU Execute");
    }
}

// Subsystem 2
class Memory {
    public void load(long position, byte[] data) {
        System.out.println("Memory Load");
    }
}

// Subsystem 3
class HardDrive {
    public byte[] read(long lba, int size) {
        return new byte[0];
    }
}

// Facade
class ComputerFacade {
    private CPU cpu;
    private Memory memory;
    private HardDrive hardDrive;

    public ComputerFacade() {
        this.cpu = new CPU();
        this.memory = new Memory();
        this.hardDrive = new HardDrive();
    }

    public void start() {
        cpu.freeze();
        memory.load(0, hardDrive.read(0, 0));
        cpu.jump(0);
        cpu.execute();
    }
}
