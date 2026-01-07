INSERT INTO items ( title, description, img_path,  price) VALUES
                                                                 ( 'ball', 'The ball description', 'images/ball.jpg', 500),
                                                                 ( 'car', 'The car description', 'images/car.jpg', 50000),
                                                                 ( 'computer', 'The computer description', 'images/computer.jpg', 15000);


INSERT INTO orders ( total_sum) VALUES
                                       ( 1000),
                                       ( 50000),
                                       (45000);


INSERT INTO orders_items (item_id, order_id,history_price,count) VALUES
                                                                     (1, 1,500,2),
                                                                     (2, 2,50000,1),
                                                                     (3, 3,15000,3);