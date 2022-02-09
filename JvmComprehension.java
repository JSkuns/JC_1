public class JvmComprehension {

    public static void main(String[] args) {
        int i = 1;                      // 1
        Object o = new Object();        // 2
        Integer ii = 2;                 // 3
        printAll(o, i, ii);             // 4
        System.out.println("finished"); // 7
    }

    private static void printAll(Object o, int i, Integer ii) {
        Integer uselessVar = 700;                   // 5
        System.out.println(o.toString() + i + ii);  // 6
    }
}

/*
*   Изначально:
*   (Stack: null) (Heap: null) (Metaspace: null)
*   Затем в Metaspace передаются данные о классе JvmComprehension
*   (Metaspace изначально неограничен, но его можно ограничить вручную)
    (размер Metaspace - от Xms96m до Xmx1g)
*
*   В stack создаётся frame метода main()
*   (размер stack memory - Xss512k)
*
*
* 1. int i = 1;
*   Будет подгружен в Stack memory, в frame метода main
*   (Stack:
*       main(int i = 1);
*       )
*   (Heap:
*       null;
*       )
*   (Metaspace:
*       JvmComprehension.class;
*       )
*
*
* 2. Object o = new Object();
*   Подгрузка нового класса при помощи системы загрузчиков классов ClassLoader.
*       1) Application ClassLoader - загружает все классы и jar-файлы уровня приложения
*       2) Platform ClassLoader - загружает расширения основных классов Java из библиотеки расширений JDK
*       3) Bootstrap ClassLoader - загружает системные классы (типа java.lang.* или java.util.*)
*   Если не удалось подгрузить, то выдаёт ClassNotFoundException - класс не найден.
*   (Stack:
*       main(o); - он ссылается на объект Object в куче (heap), и также принадлежит фрейму main
*       main(int i = 1);
*       )
*   (Heap:
*       Object; - подгружается с помощью ClassLoader
*       )
*   (Metaspace:
*       JvmComprehension.class;
*       )
*
*
* 3. Integer ii = 2;
*   Будет подгружен в Stack memory, в frame метода main
*   (Stack:
*       main(ii); - он ссылается на объект Integer в куче (heap), и также принадлежит фрейму main
*       main(o);
*       main(int i = 1);
*       )
*   (Heap:
*       Integer; - класс-обёртка подгружается с помощью ClassLoader
*       Object;
*       )
*   (Metaspace:
*       JvmComprehension.class;
*       )
*
*
* 4. printAll(o, i, ii);
*   Будет создан новый фрейм в stack memory, и в него добавлены поля -> o, i, ii
*   (Stack:
*       printAll(ii); - будет передан из фрейма main в этот фрейм
*       printAll(i); - будет передан из фрейма main в этот фрейм
*       printAll(o); - будет ссылаться на объект из кучи
*       main(ii);
*       main(o);
*       main(int i = 1);
*       )
*   (Heap:
*       Integer;
*       Object;
*       )
*   (Metaspace:
*       JvmComprehension.class;
*       )
*
*
* 5. Integer uselessVar = 700;
*   Будет подгружен в Stack memory, в frame метода printAll
*   (Stack:
*       printAll(uselessVar); - ссылается на Integer из кучи, создаётся новый объект класса-обёртки
*       printAll(ii);
*       printAll(i);
*       printAll(o);
*       main(ii);
*       main(o);
*       main(int i = 1);
*       )
*   (Heap:
*       Integer;
*       Object;
*       )
*   (Metaspace:
*       JvmComprehension.class;
*       )
*
*
* 6. System.out.println(o.toString() + i + ii);
*   Создаётся новые фреймы println, toString, , в которые передаются поля o, i, ii
*   (Stack:
*       concat(ii);
*       concat(i);
*       concat(toString(o)); - фрейм, для объединения строк
*       toString(o); - создаётся фрейм, на основе ссылочного объекта Object
*       println(o.toString() + i + ii); - ссылается на PrintStream из кучи, создаётся фрейм println, также передаются данные из ранее объявленных o, i, ii
*       printAll(out); - ссылается на System из кучи, статическая переменная класса System
*       printAll(uselessVar);
*       printAll(ii);
*       printAll(i);
*       printAll(o);
*       main(ii);
*       main(o);
*       main(int i = 1);
*       )
*   (Heap:
*       String; - подгружается с помощью ClassLoader
*       PrintStream; - подгружается с помощью ClassLoader
*       System; - подгружается с помощью ClassLoader
*       Integer;
*       Object;
*       )
*   (Metaspace:
*       JvmComprehension.class;
*       )
*
*   После того как закончился метод printAll() фрейм выгружается из стэка,
*       значимые поля удаляются окончательно,
*       у ссылочных полей остаются ссылки в куче
*
*   Соответственно удалится из стэка:
*       фрейм concat;
*       фрейм toString;
*       фрейм println;
*       фрейм printAll;
*
*
* 7. System.out.println("finished");
*   (Stack:
*       println("finished"); - ссылается на PrintStream из кучи, создаётся фрейм println, передаётся в этот фрейм String
*       main(out); - ссылается на System из кучи, статическая переменная класса System
*       main(ii);
*       main(o);
*       main(int i = 1);
*       )
*   (Heap:
*       String;
*       PrintStream;
*       System;
*       Integer;
*       Object;
*       )
*   (Metaspace:
*       JvmComprehension.class;
*       )
*
*   Сборка мусора мертвых классов и загрузчиков классов запускается, когда использование метаданных класса достигает «MaxMetaspaceSize».
*   Также сборрщик мусора удаляет объекты, после того как у объекта не будет связей.
*   Соответственно когда достигнется конец метода main, сборщик удалит объект 'o'
*
*   После выполнения всей программы удалятся все данные из стэка. Затем удалятся объекты из heap.
*   И после закрытия программы подчистится metaspace.
*
* */
