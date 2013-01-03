package io.trygvis.esper.testing.util.sql;

import fj.data.*;

import java.sql.*;

public abstract class SqlOption<A> {
    public static <A> SqlOption<A> none() {
        return new None<>();
    }

    public static <A> SqlOption<A> some(A a) {
        return new Some<>(a);
    }

    public static SqlOption<ResultSet> fromRs(ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return none();
        }

        return some(rs);
    }

    // -----------------------------------------------------------------------
    //
    // -----------------------------------------------------------------------

    public abstract <B> SqlOption<B> map(SqlF<A, B> f) throws SQLException;

    public <B> SqlOption<B> flatMap(SqlF<A, SqlOption<B>> f) throws SQLException {
        SqlOption<SqlOption<B>> x = map(f);

        if (x.isNone()) {
            return none();
        }

        return x.get();
    }

    public abstract A get() throws SQLException;

    public abstract boolean isSome();

    public boolean isNone() {
        return !isSome();
    }

    public abstract A getOrElse(A a);

    public abstract A getOrElse(SqlP0<A> p) throws SQLException;

    public abstract Option<A> toFj();

    public static <A> SqlOption<A> fromNull(A a) {
        if (a != null) {
            return some(a);
        } else {
            return none();
        }
    }

    // -----------------------------------------------------------------------
    //
    // -----------------------------------------------------------------------

    private static class None<A> extends SqlOption<A> {
        public <B> SqlOption<B> map(SqlF<A, B> f) {
            return none();
        }

        public A get() throws SQLException {
            throw new SQLException("get() on None");
        }

        public boolean isSome() {
            return false;
        }

        public A getOrElse(A a) {
            return a;
        }

        public A getOrElse(SqlP0<A> p) throws SQLException {
            return p.apply();
        }

        public Option<A> toFj() {
            return Option.none();
        }

        public String toString() {
            return "None";
        }
    }

    private static class Some<A> extends SqlOption<A> {
        private final A a;

        private Some(A a) {
            this.a = a;
        }

        public <B> SqlOption<B> map(SqlF<A, B> f) throws SQLException {
            return some(f.apply(a));
        }

        public A get() {
            return a;
        }

        public boolean isSome() {
            return true;
        }

        public A getOrElse(A a) {
            return this.a;
        }

        public A getOrElse(SqlP0<A> p) throws SQLException {
            return a;
        }

        public Option<A> toFj() {
            return Option.some(a);
        }

        public String toString() {
            return "Some(" + a + ")";
        }
    }
}
