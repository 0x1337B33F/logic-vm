package logictype;

/**
 *
 * @param <A>
 * @param <B>
 *
 * @author Lochie
 */
public class Pair<A, B> {

    volatile A a;
    volatile B b;

    public Pair() {
    }

    public Pair(A a, B b) {
	this.a = a;
	this.b = b;
    }

    public A a() {
	return a;
    }

    public B b() {
	return b;
    }

    public A setA(A a) {
	A oldA = this.a;
	this.a = a;
	return oldA;
    }

    public B setB(B b) {
	B oldB = this.b;
	this.b = b;
	return oldB;
    }

}
