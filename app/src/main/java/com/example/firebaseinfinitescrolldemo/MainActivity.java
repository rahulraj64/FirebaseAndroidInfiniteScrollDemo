package com.example.firebaseinfinitescrolldemo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button insert;
    List<Book> mTempBooks = new ArrayList<>();
    List<Book> mBooks = new ArrayList<>();
    String mLastKey;
    RecyclerView mBookList;
    BookAdapter bookAdapter;
    private boolean mLoadingMoreBooks = false;
    private List<Book> mMoreBooks = new ArrayList<>();
    private LinearLayoutManager mLayoutManager;
    int pastVisiblesItems, visibleItemCount, totalItemCount;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        insert = findViewById(R.id.insert);
        mBookList = findViewById(R.id.bookList);
        insert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(int i=0; i<100; i++) {
                    Book book = new Book("Title " + (i+1), "Author " + (i+1));
                    FirebaseDatabase.getInstance().getReference().push().setValue(book);
                }
            }
        });

        bookAdapter = new BookAdapter();
        mLayoutManager = new LinearLayoutManager(this);
        mBookList.setLayoutManager(mLayoutManager);
        mBookList.setAdapter(bookAdapter);
        mBookList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if(dy > 0) {
                    visibleItemCount = mLayoutManager.getChildCount();
                    totalItemCount = mLayoutManager.getItemCount();
                    pastVisiblesItems = mLayoutManager.findFirstVisibleItemPosition();
                    if (!mLoadingMoreBooks) {
                        if ( (visibleItemCount + pastVisiblesItems) >= totalItemCount) {
                            mLoadingMoreBooks = true;
                            System.out.println(" >>> end" );
                            queryMoreBooks();

                        }
                    }
                }
            }
        });
        queryBooks();
    }

    private void queryBooks() {
        Query booksQuery = FirebaseDatabase.getInstance().getReference().orderByKey().limitToLast(21);
        booksQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    Book book = snapshot.getValue(Book.class);
                    book.setNodeKey(snapshot.getKey());
                    System.out.println(">> book = " + book);
                    mTempBooks.add(book);
                    if (mTempBooks.size() == 21) {
                        mLastKey = mTempBooks.get(0).getNodeKey();
                        Collections.reverse(mTempBooks);
                        mTempBooks.remove(mTempBooks.size() - 1);
                        mBooks.addAll(mTempBooks);
                        bookAdapter.fillBooks(mBooks);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }


    private void queryMoreBooks() {
            Query imagesQuery = FirebaseDatabase.getInstance().getReference().orderByKey().endAt(mLastKey).limitToLast(21);
            imagesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Book book = snapshot.getValue(Book.class);
                        book.setNodeKey(snapshot.getKey());
                        System.out.println(">> book = " + book);
                        mMoreBooks.add(book);
                        if (mMoreBooks.size() == 21) {
                            mLastKey = mMoreBooks.get(0).getNodeKey();
                            Collections.reverse(mMoreBooks);
                            mMoreBooks.remove(mMoreBooks.size() - 1);
                            mBooks.addAll(mMoreBooks);
                            mMoreBooks.clear();
                            mLoadingMoreBooks = false;
                            bookAdapter.notifyDataSetChanged();
                            return;
                        }

                        if (mLastKey.equalsIgnoreCase(book.getNodeKey())) {
                            Collections.reverse(mMoreBooks);
                            mBooks.addAll(mMoreBooks);
                            mMoreBooks.clear();
                            mLoadingMoreBooks = true;
                            bookAdapter.onNoMoreBooks();
                            Toast.makeText(MainActivity.this, "No more data", Toast.LENGTH_SHORT).show();
                            bookAdapter.notifyDataSetChanged();
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

    }


    static class BookAdapter extends RecyclerView.Adapter<BookViewHolder> {

        private List<Book> books = new ArrayList<>();

        public void fillBooks(List<Book> books) {
            this.books = books;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new BookViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_book, null));
        }

        @Override
        public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
            holder.title.setText(books.get(position).title);
            holder.author.setText(books.get(position).author);
        }

        @Override
        public int getItemCount() {
            return books.size();
        }

        public void onNoMoreBooks() {

        }
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView author;
        TextView title;
        public BookViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            author = itemView.findViewById(R.id.author);
        }
    }

    static class Book {
        String title;
        String author;
        String nodeKey;

        public Book() {}

        public Book(String title, String author) {
            this.title = title;
            this.author = author;
        }

        public void setNodeKey(String nodeKey) {
            this.nodeKey = nodeKey;
        }

        public String getNodeKey() {
            return nodeKey;
        }

        public String getTitle() {
            return title;
        }

        public String getAuthor() {
            return author;
        }

        @Override
        public String toString() {
            return "Book{" +
                    "title='" + title + '\'' +
                    ", author='" + author + '\'' +
                    ", nodeKey='" + nodeKey + '\'' +
                    '}';
        }
    }
}
