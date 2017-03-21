package org.stagemonitor.vertx;

import net.bytebuddy.jar.asm.Attribute;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;

public class ModifiersMethodVisitor extends MethodVisitor {

	public ModifiersMethodVisitor(MethodVisitor mv) {
		super(Opcodes.ASM5, mv);
	}

	@Override
	public void visitAttribute(Attribute attr) {
		System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!" + attr.type);
		super.visitAttribute(attr);
	}
}
