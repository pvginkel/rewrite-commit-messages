# Rewrite commit messages

This project allows you to rewrite Git commit messages in a fast and simple way using Java.

This project contains a very simple implementation of `git filter-branch`, limited to rewriting the commits itself. It does not contain support for rewriting the file trees, just the commits themselves.

The purpose of this project is to allow you to write a small bit of Java code that takes a [`CommitBuilder`](http://download.eclipse.org/jgit/site/4.9.0.201710071750-r/apidocs/org/eclipse/jgit/lib/CommitBuilder.html) and [`RevCommit`](http://download.eclipse.org/jgit/site/4.9.0.201710071750-r/apidocs/org/eclipse/jgit/revwalk/RevCommit.html) and change any properties of the `CommitBuilder` as you'd like. The purpose of the project is to prepare the `CommitBuilder`, ensure that parent's are updated to the new commit and all refs (i.e. branches and tags) are correctly rewritten.

## Usage

To use this project, fork it in GitHub and clone it. After that, in your favorite Java IDE, open the `App.java` file and write your code in the `rewriteCommit` method, e.g.:

```java
private static void rewriteCommit(RevCommit commit, CommitBuilder builder) {
    ///////////////////////////////////////////////////////////////////////
    // Rewrite the commit here!
    ///////////////////////////////////////////////////////////////////////
    
    builder.setMessage("CHANGED: " + builder.getMessage());
}
```

The above example shows how you can change just the commit message. You can use similar code to change the author and/or the committer.

## License

This project is licensed under the LGPL v3 license.