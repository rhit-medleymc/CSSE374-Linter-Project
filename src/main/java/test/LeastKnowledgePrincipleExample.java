package test;

public class LeastKnowledgePrincipleExample {
        void ok() {
            helper();
        }

        void tests(B b) {
            A a = new A();
            b.c(); // should be okay
            a.getB(); // should be okay
            a.getB().doSomething(); // should be flagged (method-call result used)
            a.getB().getC().doIt(); // should be flagged (chained calls)
            this.getA().b.c(); // should be flagged (chained field access)
        }

        private A getA() {
            return new A();
        }

        private void helper(){

        }

        public class A {
            public B b;
            public A(){
                this.b = new B();
            }
            public B getB(){
                return b;
            }
        }

        public class B{
            public B(){
            }

            public void c() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'c'");
            }

            public void doSomething() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'doSomething'");
            }
            public C getC(){
                return new C();
            }
        }

        public class C{
            public C(){
            }

            public void doIt() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'doIt'");
            }
        }
    }

