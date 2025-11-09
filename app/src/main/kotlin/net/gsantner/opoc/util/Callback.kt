/*#######################################################
 *
 *   Maintained 2018-2023 by Gregor Santner <gsantner AT mailbox DOT org>
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 *
#########################################################*/
package net.gsantner.opoc.util

@Suppress("unused")
class Callback {

    interface a0 {
        fun callback()
    }

    interface a1<A> {
        fun callback(arg1: A)
    }

    interface a2<A, B> {
        fun callback(arg1: A, arg2: B)
    }

    interface a3<A, B, C> {
        fun callback(arg1: A, arg2: B, arg3: C)
    }

    interface a4<A, B, C, D> {
        fun callback(arg1: A, arg2: B, arg3: C, arg4: D)
    }

    interface a5<A, B, C, D, E> {
        fun callback(arg1: A, arg2: B, arg3: C, arg4: D, arg5: E)
    }

    interface b0 {
        fun callback(): Boolean
    }

    interface b1<A> {
        fun callback(arg1: A): Boolean
    }

    interface b2<A, B> {
        fun callback(arg1: A, arg2: B): Boolean
    }

    interface b3<A, B, C> {
        fun callback(arg1: A, arg2: B, arg3: C): Boolean
    }

    interface b4<A, B, C, D> {
        fun callback(arg1: A, arg2: B, arg3: C, arg4: D): Boolean
    }

    interface b5<A, B, C, D, E> {
        fun callback(arg1: A, arg2: B, arg3: C, arg4: D, arg5: E): Boolean
    }

    interface s0 {
        fun callback(): String
    }

    interface s1<A> {
        fun callback(arg1: A): String
    }

    interface s2<A, B> {
        fun callback(arg1: A, arg2: B): String
    }

    interface s3<A, B, C> {
        fun callback(arg1: A, arg2: B, arg3: C): String
    }

    interface s4<A, B, C, D> {
        fun callback(arg1: A, arg2: B, arg3: C, arg4: D): String
    }

    interface s5<A, B, C, D, E> {
        fun callback(arg1: A, arg2: B, arg3: C, arg4: D, arg5: E): String
    }
}
