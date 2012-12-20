# Core Concepts

* Person
* Build - an execution of a CI job. Can have a user that triggered the job, either directly or through a commit
* Commit - a change in the source control system
* Product(?) - a set of git repositories, svn trunks and jenkins jobs/builds. Many persons can be participating in
  developing the product (creates a team concept).
* Gang Programming Session - a session with multiple people working together on the same problem.

# Badges

## Unbreakable - Per Person

N builds started by U in a row that didn't break the build

## Well Tested - Per Product

Product P has increased the number of tests the last N (commits|days).

## Gang Programmer

Level 1: 3 programmers
Level 2: 4 programmers
Level 3: 5 programmers
