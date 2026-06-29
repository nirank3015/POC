-- Add index for category column to optimize filtering by category
CREATE INDEX idx_product_category ON products (category);

-- Add index for price column to optimize filtering by price
CREATE INDEX idx_product_price ON products (price);