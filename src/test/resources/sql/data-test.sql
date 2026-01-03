INSERT INTO items (id, title, description, img_path,  price) VALUES
                                                                 (1000, 'test1-ball', 'Test ball description1', 'images/test-ball.jpg', 500),
                                                                 (1001, 'test1-car', 'Test car description1', 'images/test-car.jpg', 50000),
                                                                 (1002, 'test-computer', 'Test computer description', 'images/test-computer.jpg', 15000);


INSERT INTO orders (id, total_sum) VALUES
                                       (1000, 1000),
                                       (1001, 50000),
                                       (1002, 45000);


INSERT INTO orders_items (item_id, order_id,history_price,count) VALUES
                                                                     (1000, 1000,500,2),
                                                                     (1001, 1001,50000,1),
                                                                     (1002, 1002,15000,3);