package com.github.pvginkel.rewriteComitMessages;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class App {
    public static void main(String[] args) throws IOException, GitAPIException {
        if (args.length == 0) {
            System.err.println("Missing path to repository");
            return;
        }

        FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder()
            .setMustExist(true)
            .findGitDir(new File(args[0]));

        try (
            Repository repo = repoBuilder.build();
            Git git = new Git(repo)
        ) {
            List<RevCommit> commits = new ArrayList<>();

            for (RevCommit revCommit : git.log().all().call()) {
                commits.add(revCommit);
            }

            Map<ObjectId, ObjectId> idMap = new HashMap<>();

            for (int i = commits.size() - 1; i >= 0; i--) {
                RevCommit commit = commits.get(i);

                System.out.println("Rewriting " + commit.getId());

                CommitBuilder builder = new CommitBuilder();
                builder.setAuthor(commit.getAuthorIdent());
                builder.setCommitter(commit.getCommitterIdent());
                builder.setEncoding(commit.getEncoding());
                builder.setMessage(commit.getFullMessage());
                builder.setTreeId(commit.getTree());

                for (RevCommit parent : commit.getParents()) {
                    builder.addParentId(parent.getId());
                }

                rewriteCommit(commit, builder);

                ObjectId[] parents = builder.getParentIds();
                for (int j = 0; j < parents.length; j++) {
                    parents[j] = idMap.get(parents[j]);
                    if (parents[j] == null) {
                        throw new IllegalStateException("Cannot map parent ID");
                    }
                }

                try (ObjectInserter inserter = repo.newObjectInserter()) {
                    idMap.put(commit.getId(), inserter.insert(builder));
                    inserter.flush();
                }
            }

            for (Map.Entry<String, Ref> ref : repo.getAllRefs().entrySet()) {
                System.out.println("Rewriting " + ref.getValue());

                RefUpdate refUpdate = repo.updateRef(ref.getKey());
                refUpdate.setNewObjectId(idMap.get(ref.getValue().getObjectId()));

                RefUpdate.Result result = refUpdate.forceUpdate();

                switch (result) {
                    case NEW:
                    case FORCED:
                    case FAST_FORWARD:
                    case NO_CHANGE:
                        break;
                    default:
                        throw new IllegalStateException("Failed to update branch '" + ref.getKey() + "' because '" + result + "'");
                }
            }
        }
    }

    private static void rewriteCommit(RevCommit commit, CommitBuilder builder) {
        ///////////////////////////////////////////////////////////////////////
        // Rewrite the commit here!
        ///////////////////////////////////////////////////////////////////////
    }
}
