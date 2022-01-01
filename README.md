# Installing

```
git pull
```

- Place an image within `\app\src\main\res\drawable\sample.png`
- Ensure that `\app\src\main\res\layout\activity.xml` has an `ImageView` which points to the new image

```
<ImageView
    android:id="@+id/imageView"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:layout_constraintBottom_toTopOf="@+id/textView"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:srcCompat="@drawable/sample" />
```