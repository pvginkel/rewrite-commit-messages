package com.github.pvginkel.rewriteComitMessages;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.notes.Note;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            Map<ObjectId, String> noteMap = new HashMap<>();

            for (Note note : git.notesList().call()) {
                noteMap.put(
                    note.toObjectId(),
                    new String(repo.open(note.getData()).getBytes(), StandardCharsets.UTF_8)
                );
            }

            List<RevCommit> commits = new ArrayList<>();

            try (RevWalk walk = new RevWalk(repo)) {
                for (Ref ref : repo.getRefDatabase().getRefs(RefDatabase.ALL).values()) {
                    if (ref.getName().equals(Constants.R_NOTES_COMMITS)) {
                        continue;
                    }
                    if (!ref.isPeeled()) {
                        ref = repo.peel(ref);
                    }

                    ObjectId objectId = ref.getPeeledObjectId();
                    if (objectId == null) {
                        objectId = ref.getObjectId();
                    }
                    walk.markStart(walk.parseCommit(objectId));
                }

                walk.sort(RevSort.TOPO, true);
                walk.sort(RevSort.REVERSE, true);

                for (RevCommit commit : walk) {
                    commits.add(commit);
                }
            }

            Map<ObjectId, ObjectId> idMap = new HashMap<>();

            for (RevCommit commit : commits) {
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

                String oldNote = noteMap.get(commit.getId());
                NoteData note = new NoteData();
                note.data = oldNote;

                rewriteCommit(commit, builder, note);

                ObjectId[] parents = builder.getParentIds();
                for (int j = 0; j < parents.length; j++) {
                    parents[j] = idMap.get(parents[j]);
                    if (parents[j] == null) {
                        throw new IllegalStateException("Cannot map parent ID");
                    }
                }

                ObjectId newId;

                try (ObjectInserter inserter = repo.newObjectInserter()) {
                    newId = inserter.insert(builder);
                    inserter.flush();
                }

                idMap.put(commit.getId(), newId);

                try (RevWalk walk = new RevWalk(repo)) {
                    RevCommit newCommit = walk.parseCommit(newId);
                    boolean writeNote;

                    if (commit.getId().equals(newId)) {
                        writeNote = !stringEquals(oldNote, note.data);
                    } else {
                        writeNote = note.data != null;
                    }

                    if (writeNote) {
                        System.out.println("\tWriting notes");
                        if (note.data != null) {
                            git.notesAdd().setObjectId(newCommit).setMessage(note.data).call();
                        } else {
                            git.notesRemove().setObjectId(newCommit).call();
                        }
                    }
                }
            }

            for (Map.Entry<String, Ref> ref : repo.getAllRefs().entrySet()) {
                ObjectId newRefId = idMap.get(ref.getValue().getObjectId());
                if (newRefId == null) {
                    continue;
                }

                System.out.println("Rewriting " + ref.getValue());

                RefUpdate refUpdate = repo.updateRef(ref.getKey());
                refUpdate.setNewObjectId(newRefId);

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

    private static boolean stringEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static class NoteData {
        String data;
    }

    private static void rewriteCommit(RevCommit commit, CommitBuilder builder, NoteData note) {
        ///////////////////////////////////////////////////////////////////////
        // Rewrite the commit here!
        ///////////////////////////////////////////////////////////////////////
    }
}
